package com.commercepaymentsystem.domain.payment.dto;

public record PaymentConfirmCommand(
	String paymentId,
	Long memberId
) {

	public static PaymentConfirmCommand of(String paymentId, Long memberId) {
		return new PaymentConfirmCommand(paymentId, memberId);
	}
}
