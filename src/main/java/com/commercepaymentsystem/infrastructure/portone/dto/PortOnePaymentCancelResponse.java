package com.commercepaymentsystem.infrastructure.portone.dto;

import java.time.Instant;

public record PortOnePaymentCancelResponse(
	PortOnePaymentCancellation cancellation
) {

	public record PortOnePaymentCancellation(
		String id,
		String status,
		String pgCancellationId,
		Long totalAmount,
		String reason,
		Instant requestedAt,
		Instant cancelledAt
	) {
	}
}
