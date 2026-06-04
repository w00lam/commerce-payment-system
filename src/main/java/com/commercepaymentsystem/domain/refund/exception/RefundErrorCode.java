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
	),

	PAYMENT_NOT_FOUND(
		HttpStatus.NOT_FOUND,
		"REFUND_006",
		"결제 정보를 찾을 수 없습니다."
	),

	PAYMENT_OWNER_MISMATCH(
		HttpStatus.FORBIDDEN,
		"REFUND_007",
		"결제 소유자가 일치하지 않습니다."
	),

	INVALID_PAYMENT_STATUS(
		HttpStatus.CONFLICT,
		"REFUND_008",
		"환불할 수 없는 결제 상태입니다."
	),

	ORDER_NOT_FOUND(
		HttpStatus.NOT_FOUND,
		"REFUND_009",
		"주문 정보를 찾을 수 없습니다."
	),

	INVALID_REFUND_ITEM(
		HttpStatus.BAD_REQUEST,
		"REFUND_010",
		"유효하지 않은 환불 상품입니다."
	),

	PORTONE_REFUND_FAILED(
		HttpStatus.BAD_GATEWAY,
		"REFUND_011",
		"포트원 환불 요청에 실패했습니다."
	);

	private final HttpStatus status;
	private final String code;
	private final String message;
}
