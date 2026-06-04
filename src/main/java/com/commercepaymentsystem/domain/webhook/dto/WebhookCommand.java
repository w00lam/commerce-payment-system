package com.commercepaymentsystem.domain.webhook.dto;

public record WebhookCommand(
	WebhookEventType eventType,
	String paymentId
) {
}
