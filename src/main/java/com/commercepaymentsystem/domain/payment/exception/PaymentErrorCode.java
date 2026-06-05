package com.commercepaymentsystem.domain.payment.exception;

import org.springframework.http.HttpStatus;

import com.commercepaymentsystem.global.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentErrorCode implements ErrorCode {

	INVALID_AMOUNT(
		HttpStatus.BAD_REQUEST,
		"PAYMENT_001",
		"유효하지 않은 결제 금액입니다."
	),

	INVALID_MEMBER_ID(
		HttpStatus.BAD_REQUEST,
		"PAYMENT_002",
		"유효하지 않은 회원 ID입니다."
	),

	INVALID_ORDER_ID(
		HttpStatus.BAD_REQUEST,
		"PAYMENT_003",
		"유효하지 않은 주문 ID입니다."
	),

	PAYMENT_ID_GENERATION_FAILED(
		HttpStatus.INTERNAL_SERVER_ERROR,
		"PAYMENT_004",
		"결제 ID 생성에 실패했습니다."
	),

	INVALID_PAYMENT_ID(
		HttpStatus.BAD_REQUEST,
		"PAYMENT_005",
		"유효하지 않은 결제 ID입니다."
	),

	PAYMENT_NOT_FOUND(
		HttpStatus.NOT_FOUND,
		"PAYMENT_006",
		"결제 정보를 찾을 수 없습니다."
	),

	PAYMENT_OWNER_MISMATCH(
		HttpStatus.FORBIDDEN,
		"PAYMENT_007",
		"결제 소유자가 일치하지 않습니다."
	),

	INVALID_PAYMENT_STATUS(
		HttpStatus.CONFLICT,
		"PAYMENT_008",
		"유효하지 않은 결제 상태입니다."
	),

	PORTONE_PAYMENT_VERIFICATION_FAILED(
		HttpStatus.BAD_REQUEST,
		"PAYMENT_009",
		"포트원 결제 검증에 실패했습니다."
	),

	PORTONE_PAYMENT_REQUEST_FAILED(
		HttpStatus.BAD_GATEWAY,
		"PAYMENT_010",
		"포트원 결제 요청에 실패했습니다."
	);

	private final HttpStatus status;
	private final String code;
	private final String message;
}
