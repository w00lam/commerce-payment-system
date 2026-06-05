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
		"주문 정보를 찾을 수 없습니다."
	),

	INVALID_ORDER_STATUS(
		HttpStatus.BAD_REQUEST,
		"ORDER_002",
		"유효하지 않은 주문 상태입니다."
	),

	EMPTY_ORDER_ITEM(
		HttpStatus.BAD_REQUEST,
		"ORDER_003",
		"주문 상품이 비어 있습니다."
	),

	INVALID_POINT_AMOUNT(
		HttpStatus.BAD_REQUEST,
		"ORDER_004",
		"유효하지 않은 포인트 금액입니다."
	),

	ORDER_OWNER_MISMATCH(
		HttpStatus.FORBIDDEN,
		"ORDER_005",
		"주문 소유자가 일치하지 않습니다."
	),

	ORDER_ITEM_NOT_FOUND(
		HttpStatus.NOT_FOUND,
		"ORDER_006",
		"주문 상품을 찾을 수 없습니다."
	);

	private final HttpStatus status;
	private final String code;
	private final String message;
}
