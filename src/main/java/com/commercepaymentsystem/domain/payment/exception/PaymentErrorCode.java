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
	);

	private final HttpStatus status;
	private final String code;
	private final String message;
}
