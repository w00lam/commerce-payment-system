package com.commercepaymentsystem.domain.order.exception;

import org.springframework.http.HttpStatus;

import com.commercepaymentsystem.global.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderErrorCode implements ErrorCode {

	ORDER_NOT_FOUND(
		HttpStatus.NOT_FOUND,
		"ORDER_001",
		"주문을 찾을 수 없습니다."
	),

	INVALID_ORDER_STATUS(
		HttpStatus.BAD_REQUEST,
		"ORDER_002",
		"유효하지 않은 주문 상태입니다."
	);

	private final HttpStatus status;
	private final String code;
	private final String message;
}