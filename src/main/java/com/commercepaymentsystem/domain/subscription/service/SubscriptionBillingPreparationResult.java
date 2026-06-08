package com.commercepaymentsystem.domain.subscription.service;

public record SubscriptionBillingPreparationResult(
	Status status,
	PreparedSubscriptionBilling billing
) {
	public enum Status {
		READY,
		SKIPPED
	}

	public static SubscriptionBillingPreparationResult ready(PreparedSubscriptionBilling billing) {
		return new SubscriptionBillingPreparationResult(Status.READY, billing);
	}

	public static SubscriptionBillingPreparationResult skipped() {
		return new SubscriptionBillingPreparationResult(Status.SKIPPED, null);
	}

	public boolean isReady() {
		return status == Status.READY;
	}
}
