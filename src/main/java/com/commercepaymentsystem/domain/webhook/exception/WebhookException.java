package com.commercepaymentsystem.domain.webhook.exception;

import com.commercepaymentsystem.global.exception.BusinessException;

public class WebhookException extends BusinessException {

	public WebhookException(WebhookErrorCode errorCode) {
		super(errorCode);
	}

	public WebhookException(WebhookErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}
