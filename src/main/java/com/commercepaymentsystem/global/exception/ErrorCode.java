package com.commercepaymentsystem.global.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	// Common
	INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON_001", "입력값이 올바르지 않습니다."),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_002", "서버 내부 오류가 발생했습니다."),

	// Member
	MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_001", "회원을 찾을 수 없습니다."),
	DUPLICATE_EMAIL(HttpStatus.CONFLICT, "MEMBER_002", "이미 존재하는 이메일입니다."),
	INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "MEMBER_003", "이메일 또는 비밀번호가 올바르지 않습니다."),

	// Product
	PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT_001", "상품을 찾을 수 없습니다."),
	INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "PRODUCT_002", "재고가 부족합니다."),
	INVALID_PRICE(HttpStatus.BAD_REQUEST, "PRODUCT_003", "가격은 0 이상이어야 합니다."),
	INVALID_STOCK(HttpStatus.BAD_REQUEST, "PRODUCT_004", "재고는 0 이상이어야 합니다."),

	// Cart
	CART_EMPTY(HttpStatus.BAD_REQUEST, "CART_001", "장바구니가 비어있습니다."),
	CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "CART_002", "장바구니 항목을 찾을 수 없습니다."),
	INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "CART_003", "수량은 1 이상이어야 합니다."),

	// Order
	ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_001", "주문을 찾을 수 없습니다."),
	INVALID_ORDER_STATUS(HttpStatus.BAD_REQUEST, "ORDER_002", "유효하지 않은 주문 상태 변경입니다."),

	// Payment
	PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PAYMENT_001", "결제 정보를 찾을 수 없습니다."),
	PAYMENT_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "PAYMENT_002", "결제 금액이 일치하지 않습니다."),
	INVALID_PAYMENT_STATUS(HttpStatus.BAD_REQUEST, "PAYMENT_003", "유효하지 않은 결제 상태 변경입니다."),
	PAYMENT_NOT_PAID(HttpStatus.BAD_REQUEST, "PAYMENT_004", "PG사 결제가 완료되지 않았습니다."),
	ALREADY_PROCESSED_PAYMENT(HttpStatus.CONFLICT, "PAYMENT_005", "이미 처리된 결제입니다."),

	// Webhook
	INVALID_WEBHOOK_SIGNATURE(HttpStatus.UNAUTHORIZED, "WEBHOOK_001", "웹훅 서명이 유효하지 않습니다."),
	WEBHOOK_EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "WEBHOOK_002", "웹훅 이벤트를 찾을 수 없습니다."),

	// Auth
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_001", "인증이 필요합니다."),
	INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_002", "유효하지 않은 토큰입니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
