package com.commercepaymentsystem.domain.payment.dto;

public record PaymentConfirmCommand(
	String paymentId,
	Long memberId
) {
}
