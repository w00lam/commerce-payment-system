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
	),

	EMPTY_ORDER_ITEM(
		HttpStatus.BAD_REQUEST,
		"ORDER_003",
		"주문할 장바구니 상품이 없습니다."
	),

	INVALID_POINT_AMOUNT(
		HttpStatus.BAD_REQUEST,
		"ORDER_004",
		"사용 포인트가 주문 총액보다 큽니다."
	);

	private final HttpStatus status;
	private final String code;
	private final String message;
}