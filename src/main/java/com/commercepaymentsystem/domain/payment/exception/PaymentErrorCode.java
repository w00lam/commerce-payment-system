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
		"Invalid payment amount."
	),

	INVALID_MEMBER_ID(
		HttpStatus.BAD_REQUEST,
		"PAYMENT_002",
		"Invalid payment member id."
	),

	INVALID_ORDER_ID(
		HttpStatus.BAD_REQUEST,
		"PAYMENT_003",
		"Invalid payment order id."
	),

	PAYMENT_ID_GENERATION_FAILED(
		HttpStatus.INTERNAL_SERVER_ERROR,
		"PAYMENT_004",
		"Failed to generate payment id."
	),

	INVALID_PAYMENT_ID(
		HttpStatus.BAD_REQUEST,
		"PAYMENT_005",
		"Invalid payment id."
	),

	PAYMENT_NOT_FOUND(
		HttpStatus.NOT_FOUND,
		"PAYMENT_006",
		"Payment not found."
	),

	PAYMENT_OWNER_MISMATCH(
		HttpStatus.FORBIDDEN,
		"PAYMENT_007",
		"Payment owner mismatch."
	),

	INVALID_PAYMENT_STATUS(
		HttpStatus.CONFLICT,
		"PAYMENT_008",
		"Invalid payment status."
	),

	PORTONE_PAYMENT_VERIFICATION_FAILED(
		HttpStatus.BAD_REQUEST,
		"PAYMENT_009",
		"PortOne payment verification failed."
	),

	PORTONE_PAYMENT_REQUEST_FAILED(
		HttpStatus.BAD_GATEWAY,
		"PAYMENT_010",
		"PortOne payment request failed."
	);

	private final HttpStatus status;
	private final String code;
	private final String message;
}
