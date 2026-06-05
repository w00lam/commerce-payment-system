package com.commercepaymentsystem.domain.webhook.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.commercepaymentsystem.domain.webhook.entity.WebhookEvent;
import com.commercepaymentsystem.domain.webhook.entity.WebhookStatus;
import com.commercepaymentsystem.domain.webhook.repository.WebhookRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WebhookEventRecorder {

	private final WebhookRepository webhookRepository;

	/**
	 * 비즈니스 부수 효과가 실행되기 전에 수신 웹훅을 별도 트랜잭션으로 저장합니다.
	 *
	 * <p>즉시 flush 하여 중복 이벤트 ID를 빠르게 감지하고, 서비스 계층에서 이를 멱등한
	 * 중복 응답으로 변환할 수 있게 합니다.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public WebhookEvent receive(
		String eventId,
		String paymentId,
		String eventType,
		String payload
	) {
		return webhookRepository.saveAndFlush(WebhookEvent.receive(
			eventId,
			paymentId,
			eventType,
			payload
		));
	}

	/**
	 * 결제 또는 환불 처리와 독립된 트랜잭션에서 실패 결과를 저장합니다.
	 *
	 * <p>이 호출을 유발한 예외로 비즈니스 트랜잭션이 이미 롤백됐더라도, 재시도 가능한
	 * 실패 이력은 남길 수 있습니다.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void fail(String eventId, String resultMessage) {
		webhookRepository.findById(eventId)
			.ifPresent(event -> event.fail(resultMessage));
	}

	@Transactional(readOnly = true)
	public WebhookStatus findStatus(String eventId) {
		return webhookRepository.findById(eventId)
			.map(WebhookEvent::getStatus)
			.orElse(WebhookStatus.IGNORED);
	}
}
