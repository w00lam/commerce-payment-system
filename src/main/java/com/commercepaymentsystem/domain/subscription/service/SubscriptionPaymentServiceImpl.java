package com.commercepaymentsystem.domain.subscription.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.commercepaymentsystem.infrastructure.portone.client.PortOneClient;
import com.commercepaymentsystem.infrastructure.portone.dto.PortOneBillingKeyPaymentRequest;
import com.commercepaymentsystem.infrastructure.portone.dto.PortOnePaymentResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionPaymentServiceImpl implements SubscriptionPaymentService {

	private final PortOneClient portOneClient;

	/**
	 * PortOne V2 빌링키 결제 API를 호출하여 실제 결제를 진행합니다.
	 */
	@Override
	public PaymentResult pay(String billingKey, Long amount, String orderName) {
		String paymentId = "sub-pay-" + UUID.randomUUID();

		PortOneBillingKeyPaymentRequest request = new PortOneBillingKeyPaymentRequest(
			billingKey,
			orderName,
			new PortOneBillingKeyPaymentRequest.PortOneBillingKeyPaymentAmount(amount),
			"KRW",
			new PortOneBillingKeyPaymentRequest.PortOneBillingKeyCustomer("system-recurring")
		);

		try {
			PortOnePaymentResponse response = portOneClient.payWithBillingKey(paymentId, request);
			return PaymentResult.succeed(response.id());
		} catch (Exception e) {
			log.error("PortOne 정기 결제 호출 실패 - paymentId: {}, reason: {}", paymentId, e.getMessage());
			return PaymentResult.fail(e.getMessage());
		}
	}
}
