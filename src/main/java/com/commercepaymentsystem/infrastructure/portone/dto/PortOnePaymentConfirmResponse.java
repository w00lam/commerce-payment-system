package com.commercepaymentsystem.infrastructure.portone.dto;

import java.time.Instant;

public record PortOnePaymentConfirmResponse(
	PortOneConfirmedPaymentSummary transaction
) {

	public record PortOneConfirmedPaymentSummary(
		String pgTxId,
		Instant paidAt
	) {
	}
}
