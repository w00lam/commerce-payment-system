package com.commercepaymentsystem.domain.product.exception;

import org.springframework.http.HttpStatus;

import com.commercepaymentsystem.global.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProductErrorCode implements ErrorCode {

	PRODUCT_NOT_FOUND(
		HttpStatus.NOT_FOUND,
		"PRODUCT_001",
		"상품을 찾을 수 없습니다."
	),

	OUT_OF_STOCK(
		HttpStatus.BAD_REQUEST,
		"PRODUCT_002",
		"상품의 재고가 부족합니다."
	);

	private final HttpStatus status;
	private final String code;
	private final String message;
}
