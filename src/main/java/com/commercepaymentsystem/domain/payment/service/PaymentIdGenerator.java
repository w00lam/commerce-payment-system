package com.commercepaymentsystem.domain.payment.service;

import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class PaymentIdGenerator {

	private static final String PAYMENT_ID_PREFIX = "PAY-";

	public String generate() {
		return PAYMENT_ID_PREFIX + UUID.randomUUID();
	}
}
