package com.commercepaymentsystem.domain.refund.facade;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

import com.commercepaymentsystem.domain.order.entity.Order;
import com.commercepaymentsystem.domain.order.service.OrderService;
import com.commercepaymentsystem.domain.payment.entity.Payment;
import com.commercepaymentsystem.domain.payment.service.PaymentService;
import com.commercepaymentsystem.domain.point.service.PointService;
import com.commercepaymentsystem.domain.product.service.ProductService;
import com.commercepaymentsystem.domain.refund.dto.RefundCommand;
import com.commercepaymentsystem.domain.refund.dto.RefundResult;
import com.commercepaymentsystem.domain.refund.entity.Refund;
import com.commercepaymentsystem.domain.refund.entity.RefundItem;
import com.commercepaymentsystem.domain.refund.exception.RefundErrorCode;
import com.commercepaymentsystem.domain.refund.exception.RefundException;
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
	private final OrderService orderService;
	private final PointService pointService;
	private final ProductService productService;
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
			Order order = orderService.getOrderById(payment.getOrderId());
			orderService.validateOwner(order, command.memberId());
			return refundService.prepareRefund(command, payment, order);
		});

		try {
			cancelPgPayment(preparedRefund);
		} catch (PortOneException exception) {
			failRefund(preparedRefund, exception);
			throw new RefundException(RefundErrorCode.PORTONE_REFUND_FAILED, exception.getMessage());
		}

		return transactionOperations.execute(status -> {
			Payment payment = paymentService.loadAndValidatePaymentForRefund(command.paymentId(), command.memberId());
			Order order = orderService.getOrderById(payment.getOrderId());
			orderService.validateOwner(order, command.memberId());
			Refund refund = refundService.completeRefund(preparedRefund.refundId());
			List<Refund> existingRefunds = refundService.getExistingRefunds(payment.getId());
			boolean isFullRefund = refundService.isFullRefund(
				payment.getUsedPointAmount(),
				payment.getFinalPaymentAmount(),
				existingRefunds,
				refund
			);

			Map<Long, Long> productQuantities = orderService.restoreProductStock(order, refundQuantities(refund));
			productService.restoreProductStocks(productQuantities);
			restorePoint(payment, refund);
			revokeEarnedPoint(payment, refund, isFullRefund);
			updatePaymentAndOrderStatus(payment, order, isFullRefund);

			return RefundResult.from(refund, preparedRefund.portOnePaymentId());
		});
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

	private void restorePoint(Payment payment, Refund refund) {
		if (refund.getPointRefundAmount() <= 0) {
			return;
		}
		pointService.restorePoint(
			payment.getMemberId(),
			refund.getPointRefundAmount(),
			payment.getId(),
			refund.getId()
		);
	}

	private void revokeEarnedPoint(Payment payment, Refund refund, boolean isFullRefund) {
		long revokeAmount = calculateEarnedPointRevokeAmount(payment, refund, isFullRefund);
		if (revokeAmount <= 0) {
			return;
		}

		pointService.revokeEarnedPoint(
			payment.getMemberId(),
			revokeAmount,
			payment.getId(),
			refund.getId()
		);
	}

	private long calculateEarnedPointRevokeAmount(Payment payment, Refund refund, boolean isFullRefund) {
		if (payment.getEarnedPointAmount() <= 0 || payment.getFinalPaymentAmount() <= 0) {
			return 0L;
		}
		if (isFullRefund) {
			long alreadyRevokedAmount = pointService.getRevokedEarnedPointAmount(payment.getId());
			return payment.getEarnedPointAmount() - alreadyRevokedAmount;
		}

		return refund.getPgRefundAmount() * payment.getEarnedPointAmount() / payment.getFinalPaymentAmount();
	}

	private void updatePaymentAndOrderStatus(Payment payment, Order order, boolean isFullRefund) {
		paymentService.updateRefundStatus(payment, isFullRefund);

		if (isFullRefund) {
			orderService.cancelOrder(order);
		}
	}

	private Map<Long, Long> refundQuantities(Refund refund) {
		return refund.getItems().stream()
			.collect(Collectors.toMap(
				RefundItem::getOrderItemId,
				RefundItem::getRefundQuantity,
				Long::sum
			));
	}


}
