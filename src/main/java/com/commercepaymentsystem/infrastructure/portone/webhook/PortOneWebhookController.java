package com.commercepaymentsystem.infrastructure.portone.webhook;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.commercepaymentsystem.domain.webhook.dto.WebhookResponse;
import com.commercepaymentsystem.domain.webhook.exception.WebhookErrorCode;
import com.commercepaymentsystem.domain.webhook.exception.WebhookException;
import com.commercepaymentsystem.domain.webhook.service.WebhookService;
import com.commercepaymentsystem.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/payments/webhooks")
@RequiredArgsConstructor
public class PortOneWebhookController {

	private final PortOneWebhookSignatureVerifier webhookSignatureVerifier;
	private final WebhookService webhookService;
	private final ObjectMapper objectMapper;

	@PostMapping("/portone")
	@ResponseStatus(HttpStatus.OK)
	public ApiResponse<WebhookResponse> receivePortOneWebhook(
		@RequestHeader("webhook-id") String eventId,
		@RequestHeader("webhook-timestamp") String timestamp,
		@RequestHeader("webhook-signature") String signature,
		@RequestBody String payload
	) {
		webhookSignatureVerifier.verify(eventId, timestamp, signature, payload);
		PortOneWebhookRequest request = parse(payload);
		return ApiResponse.ok(webhookService.handle(eventId, request.toCommand(), payload));
	}

	private PortOneWebhookRequest parse(String payload) {
		try {
			JsonNode root = objectMapper.readTree(payload);
			return PortOneWebhookRequest.from(root);
		} catch (Exception exception) {
			throw new WebhookException(WebhookErrorCode.INVALID_PAYLOAD);
		}
	}
}
