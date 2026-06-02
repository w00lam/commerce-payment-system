package com.commercepaymentsystem.domain.order.entity;

public enum OrderStatus {

	CREATED,    // 주문 생성됨, 결제 전
	CONFIRMED,  // 결제 완료로 주문 확정
	CANCELED,   // 결제 실패/사용자 취소로 주문 취소
}