package com.commercepaymentsystem.infrastructure.portone.dto;

public record PortOnePaymentConfirmRequest(
	String paymentToken,
	String txId,
	Long totalAmount
) {
}
