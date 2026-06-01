package com.commercepaymentsystem.domain.payment.dto;

import com.commercepaymentsystem.domain.payment.entity.Payment;
import com.commercepaymentsystem.domain.payment.entity.PaymentStatus;

public record PaymentCreateResult(
	String paymentId,
	Long memberId,
	Long orderId,
	Long totalOrderAmount,
	Long usedPointAmount,
	Long finalPaymentAmount,
	PaymentStatus status
) {

	public static PaymentCreateResult from(Payment payment) {
		return new PaymentCreateResult(
			payment.getPaymentId(),
			payment.getMemberId(),
			payment.getOrderId(),
			payment.getTotalOrderAmount(),
			payment.getUsedPointAmount(),
			payment.getFinalPaymentAmount(),
			payment.getStatus()
		);
	}
}
