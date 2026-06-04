package com.commercepaymentsystem.domain.webhook.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.commercepaymentsystem.domain.webhook.dto.WebhookCommand;
import com.commercepaymentsystem.domain.webhook.dto.WebhookResponse;
import com.commercepaymentsystem.domain.webhook.entity.WebhookStatus;
import com.commercepaymentsystem.domain.webhook.exception.WebhookErrorCode;
import com.commercepaymentsystem.domain.webhook.exception.WebhookException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

	private final WebhookEventRecorder webhookEventRecorder;
	private final WebhookProcessor webhookProcessor;

	/**
	 * 웹훅 수신부터 비즈니스 처리까지 전체 흐름을 하나의 트랜잭션으로 묶지 않습니다.
	 *
	 * <p>수신 기록과 실패 기록은 별도 트랜잭션에서 커밋되므로, 결제/환불 처리 트랜잭션이
	 * 롤백되더라도 웹훅 처리 이력이 사라지지 않습니다.
	 */
	public WebhookResponse handle(String eventId, WebhookCommand command, String payload) {
		if (isBlank(eventId) || isBlank(payload)) {
			throw new WebhookException(WebhookErrorCode.INVALID_PAYLOAD);
		}
		validateCommand(command);

		try {
			webhookEventRecorder.receive(eventId, command.paymentId(), command.eventType().name(), payload);
		} catch (DataIntegrityViolationException exception) {
			log.info("Duplicate webhook event received. eventId={}", eventId);
			WebhookStatus status = webhookEventRecorder.findStatus(eventId);
			/*
			 * 중복 insert는 다른 요청이 같은 이벤트 ID를 이미 저장했다는 뜻입니다.
			 * 외부 부수 효과가 두 번 실행되지 않도록 처리 중이거나 완료된 이벤트는 현재 상태로 응답하고,
			 * 실패 이벤트만 재시도합니다.
			 */
			if (status != WebhookStatus.FAILED) {
				return new WebhookResponse(eventId, status);
			}
		}

		WebhookStatus status = processAndRecordFailure(eventId, command);
		return new WebhookResponse(eventId, status);
	}

	/**
	 * 비즈니스 처리를 실행하고, 실패 기록은 처리 트랜잭션 밖에서 남깁니다.
	 *
	 * <p>처리 트랜잭션이 롤백되면 이후 실패 기록 호출이 새 트랜잭션을 열어, 운영자가
	 * 실패 원인을 확인하고 나중에 안전하게 재시도할 수 있게 합니다.
	 */
	private WebhookStatus processAndRecordFailure(String eventId, WebhookCommand command) {
		try {
			return webhookProcessor.process(eventId, command);
		} catch (RuntimeException exception) {
			webhookEventRecorder.fail(eventId, exception.getMessage());
			throw exception;
		}
	}

	private void validateCommand(WebhookCommand command) {
		if (command == null || command.eventType() == null || isBlank(command.paymentId())) {
			throw new WebhookException(WebhookErrorCode.INVALID_PAYLOAD);
		}
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
