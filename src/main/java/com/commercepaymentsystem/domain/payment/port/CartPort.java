package com.commercepaymentsystem.domain.payment.port;

import java.util.List;

/**
 * 결제 도메인에서 장바구니 도메인의 상태를 변경하기 위해 정의한 아웃고잉 포트(Outgoing Port) 인터페이스입니다.
 */
public interface CartPort {

	/**
	 * 결제 완료되어 주문이 성사된 장바구니 항목들을 일괄 삭제합니다.
	 *
	 * @param memberId    회원 식별자
	 * @param cartItemIds 삭제할 장바구니 항목 식별자 목록
	 */
	void deleteOrderedCartItems(Long memberId, List<Long> cartItemIds);
}
