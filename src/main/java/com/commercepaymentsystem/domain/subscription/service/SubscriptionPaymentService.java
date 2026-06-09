package com.commercepaymentsystem.domain.subscription.service;

import lombok.AllArgsConstructor;
import lombok.Getter;

public interface SubscriptionPaymentService {

	PaymentResult pay(String portonePaymentId, String billingKey, Long amount, String orderName);

	@Getter
	@AllArgsConstructor
	class PaymentResult {
		private final boolean success;
		private final String portonePaymentId;
		private final String failureReason;

		public static PaymentResult succeed(String portonePaymentId) {
			return new PaymentResult(true, portonePaymentId, null);
		}

		public static PaymentResult fail(String portonePaymentId, String failureReason) {
			return new PaymentResult(false, portonePaymentId, failureReason);
		}
	}
}
