package com.commercepaymentsystem.domain.payment.port;

import java.util.List;

/**
 * 결제 도메인에서 주문 도메인의 상태를 변경하고 정보를 조회하기 위해 정의한 아웃고잉 포트(Outgoing Port) 인터페이스입니다.
 */
public interface OrderPort {

	/**
	 * 대상 주문을 확정 상태로 변경하고, 해당 주문에 매핑된 원본 장바구니 항목 식별자 목록을 반환받습니다.
	 *
	 * @param orderId  주문 식별자
	 * @param memberId 회원 식별자
	 * @return 주문 확정 결과 및 장바구니 식별자 목록을 담은 ConfirmedOrder 객체
	 */
	ConfirmedOrder confirmOrder(Long orderId, Long memberId);

	record ConfirmedOrder(List<Long> cartItemIds) {
		public ConfirmedOrder {
			cartItemIds = List.copyOf(cartItemIds);
		}
	}
}
