package com.commercepaymentsystem.domain.subscription.service;

public record PreparedSubscriptionBilling(
	Long subscriptionId,
	Long invoiceId,
	String billingKey,
	Long billingAmount,
	String planName
) {
}
