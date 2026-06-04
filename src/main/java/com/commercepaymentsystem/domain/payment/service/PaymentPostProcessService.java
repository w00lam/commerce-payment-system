package com.commercepaymentsystem.domain.payment.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commercepaymentsystem.domain.payment.entity.Payment;
import com.commercepaymentsystem.domain.payment.port.CartPort;
import com.commercepaymentsystem.domain.payment.port.OrderPort;
import com.commercepaymentsystem.domain.payment.port.PointPort;

import lombok.RequiredArgsConstructor;

/**
 * 결제 승인 완료 후 실행되는 후속 비즈니스 프로세스들을 통합 처리하는 서비스 클래스입니다.
 * 주문 확정, 포인트 사용액 차감 및 적립, 주문 완료된 장바구니 항목 삭제 작업을 단일 트랜잭션 내에서 처리합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentPostProcessService {

	private final OrderPort orderPort;
	private final PointPort pointPort;
	private final CartPort cartPort;

	/**
	 * 결제 정보를 바탕으로 후처리 프로세스(주문 확정, 포인트 처리, 장바구니 청소)를 순차적으로 실행합니다.
	 *
	 * @param payment 결제 확정 처리가 완료된 Payment 엔티티
	 */
	public void process(Payment payment) {
		OrderPort.ConfirmedOrder confirmedOrder = orderPort.confirmOrder(
			payment.getOrderId(),
			payment.getMemberId()
		);

		if (payment.getUsedPointAmount() > 0) {
			pointPort.deductUsedPoint(
				payment.getMemberId(),
				payment.getUsedPointAmount(),
				payment.getId()
			);
		}

		if (payment.getEarnedPointAmount() > 0) {
			pointPort.earnPoint(
				payment.getMemberId(),
				payment.getEarnedPointAmount(),
				payment.getId()
			);
		}

		if (!confirmedOrder.cartItemIds().isEmpty()) {
			cartPort.deleteOrderedCartItems(
				payment.getMemberId(),
				confirmedOrder.cartItemIds()
			);
		}
	}
}
