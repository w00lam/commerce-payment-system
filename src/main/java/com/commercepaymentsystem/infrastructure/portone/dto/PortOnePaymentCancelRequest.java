package com.commercepaymentsystem.infrastructure.portone.dto;

public record PortOnePaymentCancelRequest(
	Long amount,
	Long taxFreeAmount,
	String reason,
	String requester
) {
}
