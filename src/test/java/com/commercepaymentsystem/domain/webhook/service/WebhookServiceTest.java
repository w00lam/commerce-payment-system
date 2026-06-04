package com.commercepaymentsystem.domain.webhook.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import com.commercepaymentsystem.domain.webhook.dto.WebhookCommand;
import com.commercepaymentsystem.domain.webhook.dto.WebhookEventType;
import com.commercepaymentsystem.domain.webhook.dto.WebhookResponse;
import com.commercepaymentsystem.domain.webhook.entity.WebhookStatus;

class WebhookServiceTest {

	private final WebhookEventRecorder webhookEventRecorder = mock(WebhookEventRecorder.class);
	private final WebhookProcessor webhookProcessor = mock(WebhookProcessor.class);

	private final WebhookService webhookService = new WebhookService(
		webhookEventRecorder,
		webhookProcessor
	);

	@Test
	@DisplayName("Webhook service records event first and delegates processing")
	void handle_success() {
		when(webhookProcessor.process(eq("msg-123"), any(WebhookCommand.class)))
			.thenReturn(WebhookStatus.COMPLETED);

		WebhookResponse response = webhookService.handle("msg-123", paidCommand(), paidPayload());

		assertThat(response.status()).isEqualTo(WebhookStatus.COMPLETED);
		verify(webhookEventRecorder).receive("msg-123", "payment-123", "PAYMENT_PAID", paidPayload());
		verify(webhookProcessor).process(eq("msg-123"), any(WebhookCommand.class));
		verify(webhookEventRecorder, never()).fail(anyString(), anyString());
	}

	@Test
	@DisplayName("Processing failure is recorded after processor transaction rollback")
	void handle_processingFailure_recordsFailedEvent() {
		when(webhookProcessor.process(eq("msg-123"), any(WebhookCommand.class)))
			.thenThrow(new IllegalStateException("confirm failed"));

		assertThatThrownBy(() -> webhookService.handle("msg-123", paidCommand(), paidPayload()))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("confirm failed");

		verify(webhookEventRecorder).receive("msg-123", "payment-123", "PAYMENT_PAID", paidPayload());
		verify(webhookEventRecorder).fail("msg-123", "confirm failed");
	}

	@Test
	@DisplayName("Duplicate event id from concurrent insert is treated as already received")
	void handle_concurrentDuplicateInsert_returnsExistingStatus() {
		doThrow(new DataIntegrityViolationException("duplicate event id"))
			.when(webhookEventRecorder)
			.receive("msg-123", "payment-123", "PAYMENT_PAID", paidPayload());
		when(webhookEventRecorder.findStatus("msg-123")).thenReturn(WebhookStatus.RECEIVED);

		WebhookResponse response = webhookService.handle("msg-123", paidCommand(), paidPayload());

		assertThat(response.status()).isEqualTo(WebhookStatus.RECEIVED);
		verify(webhookProcessor, never()).process(anyString(), any());
		verify(webhookEventRecorder, never()).fail(anyString(), anyString());
	}

	@Test
	@DisplayName("Failed duplicate event id is retried")
	void handle_duplicateFailedEvent_retry() {
		doThrow(new DataIntegrityViolationException("duplicate event id"))
			.when(webhookEventRecorder)
			.receive("msg-123", "payment-123", "PAYMENT_PAID", paidPayload());
		when(webhookEventRecorder.findStatus("msg-123")).thenReturn(WebhookStatus.FAILED);
		when(webhookProcessor.process(eq("msg-123"), any(WebhookCommand.class)))
			.thenReturn(WebhookStatus.COMPLETED);

		WebhookResponse response = webhookService.handle("msg-123", paidCommand(), paidPayload());

		assertThat(response.status()).isEqualTo(WebhookStatus.COMPLETED);
		verify(webhookProcessor).process(eq("msg-123"), any(WebhookCommand.class));
	}

	private WebhookCommand paidCommand() {
		return new WebhookCommand(WebhookEventType.PAYMENT_PAID, "payment-123");
	}

	private String paidPayload() {
		return "payload";
	}
}
