package com.commercepaymentsystem.domain.payment.dto;

import com.commercepaymentsystem.domain.payment.entity.Payment;
import com.commercepaymentsystem.domain.payment.entity.PaymentStatus;

public record PaymentCreateResult(
	String paymentId,
	Long memberId,
	Long orderId,
	String orderName,
	Long totalOrderAmount,
	Long usedPointAmount,
	Long finalPaymentAmount,
	PaymentStatus status
) {
	public PaymentCreateResult(
		String paymentId,
		Long memberId,
		Long orderId,
		Long totalOrderAmount,
		Long usedPointAmount,
		Long finalPaymentAmount,
		PaymentStatus status
	) {
		this(
			paymentId,
			memberId,
			orderId,
			"order-" + orderId,
			totalOrderAmount,
			usedPointAmount,
			finalPaymentAmount,
			status
		);
	}

	public static PaymentCreateResult from(Payment payment) {
		return new PaymentCreateResult(
			payment.getPaymentId(),
			payment.getMemberId(),
			payment.getOrderId(),
			resolveOrderName(payment),
			payment.getTotalOrderAmount(),
			payment.getUsedPointAmount(),
			payment.getFinalPaymentAmount(),
			payment.getStatus()
		);
	}

	private static String resolveOrderName(Payment payment) {
		if (payment.getOrderName() == null || payment.getOrderName().isBlank()) {
			return "order-" + payment.getOrderId();
		}

		return payment.getOrderName();
	}
}
