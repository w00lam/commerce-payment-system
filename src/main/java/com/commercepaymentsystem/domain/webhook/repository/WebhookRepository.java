package com.commercepaymentsystem.domain.webhook.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.commercepaymentsystem.domain.webhook.entity.WebhookEvent;
import com.commercepaymentsystem.domain.webhook.entity.WebhookStatus;

public interface WebhookRepository extends JpaRepository<WebhookEvent, String> {

	Optional<WebhookEvent> findFirstByPaymentIdAndEventTypeAndStatusIn(
		String paymentId,
		String eventType,
		Iterable<WebhookStatus> statuses
	);

	boolean existsByPaymentIdAndEventTypeAndStatusIn(
		String paymentId,
		String eventType,
		Iterable<WebhookStatus> statuses
	);
}
