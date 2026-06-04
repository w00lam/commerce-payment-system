package com.commercepaymentsystem.domain.refund.facade;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

import com.commercepaymentsystem.domain.payment.entity.Payment;
import com.commercepaymentsystem.domain.payment.service.PaymentService;
import com.commercepaymentsystem.domain.refund.dto.RefundCommand;
import com.commercepaymentsystem.domain.refund.dto.RefundResult;
import com.commercepaymentsystem.domain.refund.entity.Refund;
import com.commercepaymentsystem.domain.refund.exception.RefundErrorCode;
import com.commercepaymentsystem.domain.refund.exception.RefundException;
import com.commercepaymentsystem.domain.refund.port.RefundOrderPort;
import com.commercepaymentsystem.domain.refund.port.RefundOrderPort.RefundableOrderInfo;
import com.commercepaymentsystem.domain.refund.service.RefundPostProcessService;
import com.commercepaymentsystem.domain.refund.service.RefundService;
import com.commercepaymentsystem.domain.refund.service.RefundService.PreparedRefund;
import com.commercepaymentsystem.infrastructure.portone.client.PortOneClient;
import com.commercepaymentsystem.infrastructure.portone.dto.PortOnePaymentCancelRequest;
import com.commercepaymentsystem.infrastructure.portone.exception.PortOneException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RefundFacade {

	private static final String CANCEL_REQUESTER = "CUSTOMER";

	private final RefundService refundService;
	private final PaymentService paymentService;
	private final RefundOrderPort refundOrderPort;
	private final RefundPostProcessService refundPostProcessService;
	private final PortOneClient portOneClient;
	private final TransactionOperations transactionOperations;

	/**
	 * 환불 요청을 받아서 데이터베이스 상태(재고 복구, 포인트 복원 및 회수, 결제/주문 상태 전이)를 단일 트랜잭션으로 업데이트한 뒤,
	 * 트랜잭션 외부에서 외부 PG(PortOne)에 취소 요청을 보내 정합성을 보장하는 핵심 환불 퍼사드 로직을 실행합니다.
	 *
	 * @param command 환불 명령 정보 (결제 식별자, 주문 상품 식별자 및 환불 수량 등)
	 * @return 환불 처리 결과 정보
	 */
	public RefundResult refundPayment(RefundCommand command) {
		refundService.validateCommand(command);

		PreparedRefund preparedRefund = transactionOperations.execute(status -> {
			Payment payment = paymentService.loadAndValidatePaymentForRefund(command.paymentId(), command.memberId());
			RefundableOrderInfo orderInfo = refundOrderPort.getRefundableOrder(payment.getOrderId(), command.memberId());
			return refundService.prepareRefund(command, payment, orderInfo);
		});

		try {
			cancelPgPayment(preparedRefund);
		} catch (PortOneException exception) {
			failRefund(preparedRefund, exception);
			throw new RefundException(RefundErrorCode.PORTONE_REFUND_FAILED, exception.getMessage());
		}

		// After PG cancellation succeeds, internal post-processing must leave an auditable state
		// even when this transaction rolls back.
		try {
			return transactionOperations.execute(status -> {
				Payment payment = paymentService.loadAndValidatePaymentForRefund(command.paymentId(), command.memberId());
				Refund refund = refundService.completeRefund(preparedRefund.refundId());
				boolean isFullRefund = refundService.isFullRefund(
					payment.getUsedPointAmount(),
					payment.getFinalPaymentAmount(),
					refundService.getExistingRefunds(payment.getId()),
					refund
				);

				refundPostProcessService.process(payment, payment.getOrderId(), refund, isFullRefund);
				paymentService.updateRefundStatus(payment, isFullRefund);

				return RefundResult.from(refund, preparedRefund.portOnePaymentId());
			});
		} catch (RuntimeException exception) {
			failAfterPgCancel(preparedRefund, exception);
			throw exception;
		}
	}

	private void cancelPgPayment(PreparedRefund preparedRefund) {
		if (preparedRefund.pgAmount() <= 0) {
			return;
		}

		portOneClient.cancelPayment(
			preparedRefund.portOnePaymentId(),
			new PortOnePaymentCancelRequest(
				preparedRefund.pgAmount(),
				0L,
				preparedRefund.currentPgCancellableAmount(),
				preparedRefund.reason(),
				CANCEL_REQUESTER
			)
		);
	}

	private void failRefund(PreparedRefund preparedRefund, PortOneException exception) {
		log.error(
			"PortOne refund failed. refundId={}, paymentId={}, pgAmount={}",
			preparedRefund.refundId(),
			preparedRefund.portOnePaymentId(),
			preparedRefund.pgAmount(),
			exception
		);
		transactionOperations.execute(status -> {
			refundService.failRefund(preparedRefund.refundId());
			return null;
		});
	}

	private void failAfterPgCancel(PreparedRefund preparedRefund, RuntimeException exception) {
		log.error(
			"Refund post processing failed after PortOne refund. refundId={}, paymentId={}, pgAmount={}",
			preparedRefund.refundId(),
			preparedRefund.portOnePaymentId(),
			preparedRefund.pgAmount(),
			exception
		);
		transactionOperations.execute(status -> {
			// PG refunds cannot be retried blindly, so keep them separate from ordinary failures.
			if (preparedRefund.pgAmount() > 0) {
				refundService.failPostProcess(preparedRefund.refundId());
			} else {
				refundService.failRefund(preparedRefund.refundId());
			}
			return null;
		});
	}

}
