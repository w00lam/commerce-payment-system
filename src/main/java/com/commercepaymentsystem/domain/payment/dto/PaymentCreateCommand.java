package com.commercepaymentsystem.domain.payment.dto;

public record PaymentCreateCommand(
	Long memberId,
	Long orderId,
	Long totalOrderAmount,
	Long usedPointAmount,
	Long finalPaymentAmount
) {
	public static PaymentCreateCommand of(
		Long memberId,
		Long orderId,
		Long totalOrderAmount,
		Long usedPointAmount,
		Long finalPaymentAmount
	) {
		return new PaymentCreateCommand(
			memberId,
			orderId,
			totalOrderAmount,
			usedPointAmount,
			finalPaymentAmount
		);
	}
}
