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
		"Order not found."
	),

	INVALID_ORDER_STATUS(
		HttpStatus.BAD_REQUEST,
		"ORDER_002",
		"Invalid order status."
	),

	EMPTY_ORDER_ITEM(
		HttpStatus.BAD_REQUEST,
		"ORDER_003",
		"Order item is empty."
	),

	INVALID_POINT_AMOUNT(
		HttpStatus.BAD_REQUEST,
		"ORDER_004",
		"Invalid point amount."
	),

	ORDER_OWNER_MISMATCH(
		HttpStatus.FORBIDDEN,
		"ORDER_005",
		"Order owner does not match."
	),

	ORDER_ITEM_NOT_FOUND(
		HttpStatus.NOT_FOUND,
		"ORDER_006",
		"Order item not found."
	);

	private final HttpStatus status;
	private final String code;
	private final String message;
}
