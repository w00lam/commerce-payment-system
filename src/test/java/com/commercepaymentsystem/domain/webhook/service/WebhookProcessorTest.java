package com.commercepaymentsystem.domain.webhook.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.commercepaymentsystem.domain.payment.entity.Payment;
import com.commercepaymentsystem.domain.payment.service.PaymentPostProcessService;
import com.commercepaymentsystem.domain.payment.service.PaymentService;
import com.commercepaymentsystem.domain.payment.service.PaymentService.PaymentConfirmation;
import com.commercepaymentsystem.domain.refund.service.RefundPostProcessService;
import com.commercepaymentsystem.domain.refund.service.RefundService;
import com.commercepaymentsystem.domain.webhook.dto.WebhookCommand;
import com.commercepaymentsystem.domain.webhook.dto.WebhookEventType;
import com.commercepaymentsystem.domain.webhook.entity.WebhookEvent;
import com.commercepaymentsystem.domain.webhook.entity.WebhookStatus;
import com.commercepaymentsystem.domain.webhook.repository.WebhookRepository;

class WebhookProcessorTest {

	private final WebhookRepository webhookRepository = mock(WebhookRepository.class);
	private final PaymentService paymentService = mock(PaymentService.class);
	private final PaymentPostProcessService paymentPostProcessService = mock(PaymentPostProcessService.class);
	private final RefundService refundService = mock(RefundService.class);
	private final RefundPostProcessService refundPostProcessService = mock(RefundPostProcessService.class);

	private final WebhookProcessor webhookProcessor = new WebhookProcessor(
		webhookRepository,
		paymentService,
		paymentPostProcessService,
		refundService,
		refundPostProcessService
	);

	@Test
	@DisplayName("Paid webhook confirms payment and runs post processing")
	void process_paidWebhook_success() {
		Payment payment = confirmedPayment();
		WebhookEvent event = receivedEvent("msg-123");
		when(webhookRepository.findById("msg-123")).thenReturn(Optional.of(event));
		when(webhookRepository.existsByPaymentIdAndEventTypeAndStatusIn(eq("payment-123"), eq("PAYMENT_PAID"), any()))
			.thenReturn(false);
		when(paymentService.confirmPaymentFromWebhook("payment-123"))
			.thenReturn(new PaymentConfirmation(payment, true));

		WebhookStatus status = webhookProcessor.process("msg-123", paidCommand());

		assertThat(status).isEqualTo(WebhookStatus.COMPLETED);
		assertThat(event.getStatus()).isEqualTo(WebhookStatus.COMPLETED);
		verify(paymentService).confirmPaymentFromWebhook("payment-123");
		verify(paymentPostProcessService).process(payment);
	}

	@Test
	@DisplayName("Duplicate paid webhook for the same payment is ignored")
	void process_duplicatePaymentWebhook_ignore() {
		WebhookEvent event = receivedEvent("msg-456");
		when(webhookRepository.findById("msg-456")).thenReturn(Optional.of(event));
		when(webhookRepository.existsByPaymentIdAndEventTypeAndStatusIn(eq("payment-123"), eq("PAYMENT_PAID"), any()))
			.thenReturn(true);

		WebhookStatus status = webhookProcessor.process("msg-456", paidCommand());

		assertThat(status).isEqualTo(WebhookStatus.IGNORED);
		verify(paymentService, never()).confirmPaymentFromWebhook(anyString());
		verify(paymentPostProcessService, never()).process(any());
	}

	@Test
	@DisplayName("Failed webhook event can be retried with the same event id")
	void process_failedEvent_retry() {
		Payment payment = confirmedPayment();
		WebhookEvent event = receivedEvent("msg-123");
		event.fail("previous failure");
		when(webhookRepository.findById("msg-123")).thenReturn(Optional.of(event));
		when(webhookRepository.existsByPaymentIdAndEventTypeAndStatusIn(eq("payment-123"), eq("PAYMENT_PAID"), any()))
			.thenReturn(false);
		when(paymentService.confirmPaymentFromWebhook("payment-123"))
			.thenReturn(new PaymentConfirmation(payment, true));

		WebhookStatus status = webhookProcessor.process("msg-123", paidCommand());

		assertThat(status).isEqualTo(WebhookStatus.COMPLETED);
		verify(paymentPostProcessService).process(payment);
	}

	private WebhookCommand paidCommand() {
		return new WebhookCommand(WebhookEventType.PAYMENT_PAID, "payment-123");
	}

	private WebhookEvent receivedEvent(String eventId) {
		return WebhookEvent.receive(eventId, "payment-123", "PAYMENT_PAID", "{}");
	}

	private Payment confirmedPayment() {
		Payment payment = Payment.create("payment-123", 1L, 10L, 10_000L, 2_000L, 8_000L);
		ReflectionTestUtils.setField(payment, "id", 100L);
		payment.confirm(Instant.parse("2026-06-04T00:00:00Z"));
		return payment;
	}
}
