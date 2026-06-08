package com.commercepaymentsystem.infrastructure.portone.dto;

public record PortOneBillingKeyPaymentRequest(
	String billingKey,
	String orderName,
	PortOneBillingKeyPaymentAmount amount,
	String currency,
	PortOneBillingKeyCustomer customer
) {
	public record PortOneBillingKeyPaymentAmount(
		Long total
	) {
	}

	public record PortOneBillingKeyCustomer(
		String id
	) {
	}
}
