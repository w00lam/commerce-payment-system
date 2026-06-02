package com.commercepaymentsystem.domain.point.exception;

import org.springframework.http.HttpStatus;

import com.commercepaymentsystem.global.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PointErrorCode implements ErrorCode {

	INVALID_POINT_AMOUNT(
		HttpStatus.BAD_REQUEST,
		"POINT_001",
		"포인트 금액은 0보다 커야 합니다."
	),

	INSUFFICIENT_POINT(
		HttpStatus.BAD_REQUEST,
		"POINT_002",
		"보유 포인트가 부족합니다."
	),

	PAYMENT_ID_REQUIRED(
		HttpStatus.BAD_REQUEST,
		"POINT_003",
		"결제 식별자는 필수입니다."
	);

	private final HttpStatus status;
	private final String code;
	private final String message;
}
