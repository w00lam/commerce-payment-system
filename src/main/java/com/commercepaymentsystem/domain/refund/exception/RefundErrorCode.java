package com.commercepaymentsystem.domain.refund.exception;

import org.springframework.http.HttpStatus;

import com.commercepaymentsystem.global.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RefundErrorCode implements ErrorCode {

	INVALID_PAYMENT_ID(
		HttpStatus.BAD_REQUEST,
		"REFUND_001",
		"유효하지 않은 결제 ID입니다."
	),

	INVALID_AMOUNT(
		HttpStatus.BAD_REQUEST,
		"REFUND_002",
		"유효하지 않은 환불 금액입니다."
	),

	INVALID_REASON(
		HttpStatus.BAD_REQUEST,
		"REFUND_003",
		"유효하지 않은 환불 사유입니다."
	),

	INVALID_REFUND_STATUS(
		HttpStatus.CONFLICT,
		"REFUND_004",
		"유효하지 않은 환불 상태입니다."
	),

	REFUND_AMOUNT_EXCEEDED(
		HttpStatus.CONFLICT,
		"REFUND_005",
		"환불 가능 금액을 초과했습니다."
	);

	private final HttpStatus status;
	private final String code;
	private final String message;
}
