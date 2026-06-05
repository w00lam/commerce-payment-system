package com.commercepaymentsystem.domain.webhook.entity;

import java.time.LocalDateTime;

import com.commercepaymentsystem.global.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "webhook_events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WebhookEvent extends BaseEntity {

	@Id
	@Column(name = "event_id", nullable = false, length = 100)
	private String eventId;

	@Column(name = "payment_id", length = 100)
	private String paymentId;

	@Column(name = "event_type", nullable = false, length = 100)
	private String eventType;

	@Column(name = "payload", nullable = false, columnDefinition = "TEXT")
	private String payload;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private WebhookStatus status;

	@Column(name = "result_message", length = 500)
	private String resultMessage;

	@Column(name = "received_at", nullable = false)
	private LocalDateTime receivedAt;

	@Column(name = "processed_at")
	private LocalDateTime processedAt;

	private WebhookEvent(
		String eventId,
		String paymentId,
		String eventType,
		String payload
	) {
		this.eventId = eventId;
		this.paymentId = paymentId;
		this.eventType = eventType;
		this.payload = payload;
		this.status = WebhookStatus.RECEIVED;
		this.receivedAt = LocalDateTime.now();
	}

	public static WebhookEvent receive(
		String eventId,
		String paymentId,
		String eventType,
		String payload
	) {
		return new WebhookEvent(eventId, paymentId, eventType, payload);
	}

	public void complete(String resultMessage) {
		this.status = WebhookStatus.COMPLETED;
		this.resultMessage = resultMessage;
		this.processedAt = LocalDateTime.now();
	}

	public void ignore(String resultMessage) {
		this.status = WebhookStatus.IGNORED;
		this.resultMessage = resultMessage;
		this.processedAt = LocalDateTime.now();
	}

	public void fail(String resultMessage) {
		this.status = WebhookStatus.FAILED;
		this.resultMessage = resultMessage;
		this.processedAt = LocalDateTime.now();
	}
}
