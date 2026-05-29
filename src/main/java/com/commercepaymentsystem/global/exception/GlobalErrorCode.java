package com.commercepaymentsystem.global.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GlobalErrorCode implements ErrorCode {

	INTERNAL_SERVER_ERROR(
		HttpStatus.INTERNAL_SERVER_ERROR,
		"GLOBAL_500",
		"서버 내부 오류가 발생했습니다."
	),

	INVALID_INPUT_VALUE(
		HttpStatus.BAD_REQUEST,
		"GLOBAL_400",
		"잘못된 입력값입니다."
	),

	UNAUTHORIZED(
		HttpStatus.UNAUTHORIZED,
		"GLOBAL_401",
		"인증이 필요합니다."
	),

	ACCESS_DENIED(
		HttpStatus.FORBIDDEN,
		"GLOBAL_403",
		"접근 권한이 없습니다."
	),

	INVALID_TOKEN(
		HttpStatus.UNAUTHORIZED,
		"GLOBAL_404",
		"잘못된 토큰입니다."
	);

	private final HttpStatus status;
	private final String code;
	private final String message;
}