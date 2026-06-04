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
	 * 환불 요청을 검증하고 PG 취소와 내부 후처리를 순서대로 실행합니다.
	 *
	 * 포트원 취소는 DB 트랜잭션 밖에서 먼저 실행하고, 성공한 뒤 내부 상태를 별도 트랜잭션으로 반영합니다.
	 * 포인트 전액 결제처럼 PG 취소 금액이 없으면 포트원 호출 없이 내부 후처리만 진행합니다.
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
			"PortOne 환불 요청 실패. refundId={}, paymentId={}, pgAmount={}",
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
			"PortOne 환불 후 내부 후처리 실패. refundId={}, paymentId={}, pgAmount={}",
			preparedRefund.refundId(),
			preparedRefund.portOnePaymentId(),
			preparedRefund.pgAmount(),
			exception
		);
		transactionOperations.execute(status -> {
			if (preparedRefund.pgAmount() > 0) {
				refundService.failPostProcess(preparedRefund.refundId());
			} else {
				refundService.failRefund(preparedRefund.refundId());
			}
			return null;
		});
	}
}
