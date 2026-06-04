package com.commercepaymentsystem.infrastructure.portone.webhook;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "portone.webhook")
public record PortOneWebhookProperties(
	String secret
) {
}
