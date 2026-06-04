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
		"Invalid payment id."
	),

	INVALID_AMOUNT(
		HttpStatus.BAD_REQUEST,
		"REFUND_002",
		"Invalid refund amount."
	),

	INVALID_REASON(
		HttpStatus.BAD_REQUEST,
		"REFUND_003",
		"Invalid refund reason."
	),

	INVALID_REFUND_STATUS(
		HttpStatus.CONFLICT,
		"REFUND_004",
		"Invalid refund status."
	),

	REFUND_AMOUNT_EXCEEDED(
		HttpStatus.CONFLICT,
		"REFUND_005",
		"Refund amount exceeded."
	),

	PAYMENT_NOT_FOUND(
		HttpStatus.NOT_FOUND,
		"REFUND_006",
		"Payment not found."
	),

	PAYMENT_OWNER_MISMATCH(
		HttpStatus.FORBIDDEN,
		"REFUND_007",
		"Payment owner does not match."
	),

	INVALID_PAYMENT_STATUS(
		HttpStatus.CONFLICT,
		"REFUND_008",
		"Payment is not refundable."
	),

	ORDER_NOT_FOUND(
		HttpStatus.NOT_FOUND,
		"REFUND_009",
		"Order not found."
	),

	INVALID_REFUND_ITEM(
		HttpStatus.BAD_REQUEST,
		"REFUND_010",
		"Invalid refund item."
	),

	PORTONE_REFUND_FAILED(
		HttpStatus.BAD_GATEWAY,
		"REFUND_011",
		"PortOne refund request failed."
	);

	private final HttpStatus status;
	private final String code;
	private final String message;
}
