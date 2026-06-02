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

	PAYMENT_ID_REQUIRED(
		HttpStatus.BAD_REQUEST,
		"POINT_002",
		"결제 식별자는 필수입니다."
	),

	SOURCE_HISTORY_NOT_FOUND(
		HttpStatus.NOT_FOUND,
		"POINT_003",
		"원본 차감 내역을 찾을 수 없어 복구가 불가능합니다."
	);

	private final HttpStatus status;
	private final String code;
	private final String message;
}
