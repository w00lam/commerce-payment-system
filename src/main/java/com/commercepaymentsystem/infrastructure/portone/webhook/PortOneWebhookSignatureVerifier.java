package com.commercepaymentsystem.infrastructure.portone.webhook;

import org.springframework.stereotype.Component;

import com.commercepaymentsystem.domain.webhook.exception.WebhookErrorCode;
import com.commercepaymentsystem.domain.webhook.exception.WebhookException;

import io.portone.sdk.server.errors.WebhookVerificationException;
import io.portone.sdk.server.webhook.WebhookVerifier;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PortOneWebhookSignatureVerifier {

	private final PortOneWebhookProperties webhookProperties;

	public void verify(
		String eventId,
		String timestamp,
		String signatureHeader,
		String payload
	) {
		if (isBlank(eventId) || isBlank(timestamp) || isBlank(signatureHeader) || payload == null) {
			throw new WebhookException(WebhookErrorCode.INVALID_SIGNATURE);
		}

		String secret = webhookProperties.secret();
		if (isBlank(secret)) {
			throw new WebhookException(WebhookErrorCode.INVALID_SIGNATURE, "웹훅 시크릿이 설정되지 않았습니다.");
		}

		try {
			new WebhookVerifier(secret).verify(payload, eventId, signatureHeader, timestamp);
		} catch (WebhookVerificationException | IllegalArgumentException exception) {
			throw new WebhookException(WebhookErrorCode.INVALID_SIGNATURE);
		}
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
