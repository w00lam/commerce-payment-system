package com.commercepaymentsystem.domain.payment.port;

/**
 * 결제 도메인에서 포인트 도메인의 잔액 및 내역을 변경하기 위해 정의한 아웃고잉 포트(Outgoing Port) 인터페이스입니다.
 */
public interface PointPort {

	/**
	 * 결제 시 사용한 포인트를 회원의 잔액에서 차감하고 이력을 기록합니다.
	 *
	 * @param memberId  회원 식별자
	 * @param amount    차감할 포인트 금액
	 * @param paymentId 결제 식별자 (FK)
	 */
	void deductUsedPoint(Long memberId, Long amount, Long paymentId);

	/**
	 * 결제 완료 시 발생한 적립 포인트를 회원의 잔액에 추가하고 이력을 기록합니다.
	 *
	 * @param memberId  회원 식별자
	 * @param amount    적립할 포인트 금액
	 * @param paymentId 결제 식별자 (FK)
	 */
	void earnPoint(Long memberId, Long amount, Long paymentId);
}
