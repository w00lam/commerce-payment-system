package com.commercepaymentsystem.domain.subscription.service;

import org.springframework.stereotype.Service;

@Service
public class SubscriptionPaymentOrchestrator {

	private final SubscriptionPaymentService subscriptionPaymentService;

	public SubscriptionPaymentOrchestrator(SubscriptionPaymentService subscriptionPaymentService) {
		this.subscriptionPaymentService = subscriptionPaymentService;
	}

	/**
	 * DB 트랜잭션 밖에서 외부 PG 결제를 호출하고 예외를 실패 결과로 변환합니다.
	 *
	 * <p>준비 트랜잭션과 finalizer 트랜잭션 사이의 오케스트레이션 단계이며, PG 장애가 DB
	 * 트랜잭션 롤백 경계에 섞이지 않도록 합니다.</p>
	 */
	public SubscriptionPaymentService.PaymentResult pay(
		PreparedSubscriptionBilling billing,
		String failureMessagePrefix
	) {
		try {
			return subscriptionPaymentService.pay(
				billing.billingKey(),
				billing.billingAmount(),
				billing.planName()
			);
		} catch (Exception e) {
			return SubscriptionPaymentService.PaymentResult.fail(failureMessagePrefix + ": " + e.getMessage());
		}
	}
}
