package com.commercepaymentsystem.domain.cart.exception;

import org.springframework.http.HttpStatus;

import com.commercepaymentsystem.global.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CartErrorCode implements ErrorCode {

	CART_ITEM_NOT_FOUND(
		HttpStatus.NOT_FOUND,
		"CART_001",
		"존재하지 않는 장바구니 상품입니다."
	),

	INVALID_QUANTITY(
		HttpStatus.BAD_REQUEST,
		"CART_002",
		"잘못된 수량 값입니다. 수량은 1 이상이어야 합니다."
	),

	OUT_OF_STOCK(
		HttpStatus.CONFLICT,
		"CART_003",
		"재고가 부족합니다. (변경 수량이 상품 재고를 초과함)"
	);

	private final HttpStatus status;
	private final String code;
	private final String message;
}
