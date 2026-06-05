package com.commercepaymentsystem.domain.order.entity;

public enum OrderStatus {

	CREATED{
		@Override
		public boolean canTransitTo(OrderStatus target) {return target == CONFIRMED || target == CANCELED; }
	},    // 주문 생성됨, 결제 전
	CONFIRMED{
		@Override
		public boolean canTransitTo(OrderStatus target) {
			return target == CANCELED;
		}
	},  // 결제 완료로 주문 확정
	CANCELED{
		@Override
		public boolean canTransitTo(OrderStatus target) {
			return false;
		}
	};   // 결제 실패/사용자 취소로 주문 취소

	public abstract boolean canTransitTo(OrderStatus target);
}