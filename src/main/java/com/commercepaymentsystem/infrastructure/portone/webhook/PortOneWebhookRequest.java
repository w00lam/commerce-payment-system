package com.commercepaymentsystem.infrastructure.portone.webhook;

import com.commercepaymentsystem.domain.webhook.dto.WebhookCommand;
import com.commercepaymentsystem.domain.webhook.dto.WebhookEventType;

import tools.jackson.databind.JsonNode;

public record PortOneWebhookRequest(
	String type,
	String timestamp,
	WebhookData data
) {

	private static final String TRANSACTION_PAID = "Transaction.Paid";
	private static final String TRANSACTION_CANCELLED = "Transaction.Cancelled";
	private static final String TRANSACTION_PARTIAL_CANCELLED = "Transaction.PartialCancelled";

	public static PortOneWebhookRequest from(JsonNode root) {
		JsonNode dataNode = root.path("data");
		return new PortOneWebhookRequest(
			text(root, "type"),
			text(root, "timestamp"),
			new WebhookData(
				text(dataNode, "paymentId"),
				text(dataNode, "transactionId"),
				text(dataNode, "cancellationId")
			)
		);
	}

	public WebhookCommand toCommand() {
		return new WebhookCommand(resolveEventType(), paymentId());
	}

	private WebhookEventType resolveEventType() {
		if (type == null) {
			return WebhookEventType.UNKNOWN;
		}

		return switch (type) {
			case TRANSACTION_PAID -> WebhookEventType.PAYMENT_PAID;
			case TRANSACTION_CANCELLED -> WebhookEventType.PAYMENT_CANCELLED;
			case TRANSACTION_PARTIAL_CANCELLED -> WebhookEventType.PAYMENT_PARTIAL_CANCELLED;
			default -> WebhookEventType.UNKNOWN;
		};
	}

	public String paymentId() {
		return data == null ? null : data.paymentId();
	}

	public record WebhookData(
		String paymentId,
		String transactionId,
		String cancellationId
	) {
	}

	private static String text(JsonNode node, String fieldName) {
		JsonNode value = node.path(fieldName);
		if (value.isMissingNode() || value.isNull()) {
			return null;
		}
		return value.asText();
	}
}
