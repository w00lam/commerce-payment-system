package com.commercepaymentsystem.domain.subscription.exception;

import com.commercepaymentsystem.global.exception.BusinessException;

public class SubscriptionException extends BusinessException {

	public SubscriptionException(SubscriptionErrorCode errorCode) {
		super(errorCode);
	}

	public SubscriptionException(SubscriptionErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}
