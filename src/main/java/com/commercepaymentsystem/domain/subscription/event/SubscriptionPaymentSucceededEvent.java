package com.commercepaymentsystem.domain.subscription.event;

public record SubscriptionPaymentSucceededEvent(
	Long memberId,
	Long paidAmount,
	Long invoiceId
) {
}
