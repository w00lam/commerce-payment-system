package com.commercepaymentsystem.infrastructure.portone.dto;

import java.time.Instant;

public record PortOneBillingKeyResponse(
	String billingKey,
	String status,
	Instant issuedAt,
	PortOneBillingKeyMethod method,
	PortOneCustomer customer
) {
	public record PortOneBillingKeyMethod(
		String type,
		PortOneCard card
	) {
	}

	public record PortOneCard(
		String name,
		String number,
		String bin
	) {
	}

	public record PortOneCustomer(
		String id,
		String name,
		String email,
		String phoneNumber
	) {
	}
}
