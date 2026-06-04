package com.commercepaymentsystem.domain.webhook.exception;

import org.springframework.http.HttpStatus;

import com.commercepaymentsystem.global.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WebhookErrorCode implements ErrorCode {

	INVALID_SIGNATURE(
		HttpStatus.BAD_REQUEST,
		"WEBHOOK_001",
		"유효하지 않은 웹훅 서명입니다."
	),

	INVALID_PAYLOAD(
		HttpStatus.BAD_REQUEST,
		"WEBHOOK_002",
		"유효하지 않은 웹훅 요청입니다."
	);

	private final HttpStatus status;
	private final String code;
	private final String message;
}
