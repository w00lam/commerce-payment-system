package com.commercepaymentsystem.domain.payment.dto;

public record PaymentCreateCommand(
	Long memberId,
	Long orderId,
	Long totalOrderAmount,
	Long usedPointAmount,
	Long finalPaymentAmount
) {
}
