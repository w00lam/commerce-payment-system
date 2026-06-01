package com.commercepaymentsystem.infrastructure.portone;

import java.time.Instant;

public record PortOnePaymentResponse(
	String id,
	String status,
	String transactionId,
	String orderName,
	PortOnePaymentAmount amount,
	Instant paidAt,
	PortOnePaymentFailure failure
) {

	public Long totalAmount() {
		if (amount == null) {
			return null;
		}

		return amount.total();
	}

	public record PortOnePaymentAmount(
		Long total
	) {
	}

	public record PortOnePaymentFailure(
		String reason,
		String message
	) {
	}
}
