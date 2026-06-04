package com.commercepaymentsystem.domain.webhook.dto;

import com.commercepaymentsystem.domain.webhook.entity.WebhookStatus;

public record WebhookResponse(
	String eventId,
	WebhookStatus status
) {
}
