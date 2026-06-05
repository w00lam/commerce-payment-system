package com.commercepaymentsystem.domain.subscription.service;

import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SubscriptionPaymentServiceImpl implements SubscriptionPaymentService {

	/**
	 * PG사 정기 자동 결제 API를 모사한 가상 결제 메서드입니다.
	 * 빌링키가 "FAIL_KEY"인 경우 한도 초과 오류를 반환하며, 그 외에는 가상의 결제 성공 ID를 반환합니다.
	 */
	@Override
	public PaymentResult pay(String billingKey, Long amount, String orderName) {
		if ("FAIL_KEY".equals(billingKey)) {
			return PaymentResult.fail("한도 초과 또는 유효하지 않은 카드입니다.");
		}
		
		String dummyPaymentId = "billing-pay-" + UUID.randomUUID().toString().substring(0, 8);
		return PaymentResult.succeed(dummyPaymentId);
	}
}
