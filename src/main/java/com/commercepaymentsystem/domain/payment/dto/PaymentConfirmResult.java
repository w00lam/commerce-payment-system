package com.commercepaymentsystem.domain.payment.dto;

import java.time.Instant;

import com.commercepaymentsystem.domain.payment.entity.Payment;
import com.commercepaymentsystem.domain.payment.entity.PaymentStatus;

public record PaymentConfirmResult(
	String paymentId,
	Long memberId,
	Long orderId,
	Long finalPaymentAmount,
	PaymentStatus status,
	Instant paidAt
) {

	public static PaymentConfirmResult from(Payment payment) {
		return new PaymentConfirmResult(
			payment.getPaymentId(),
			payment.getMemberId(),
			payment.getOrderId(),
			payment.getFinalPaymentAmount(),
			payment.getStatus(),
			payment.getPaidAt()
		);
	}
}
