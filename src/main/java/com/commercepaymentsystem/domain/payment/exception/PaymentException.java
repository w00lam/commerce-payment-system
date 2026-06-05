package com.commercepaymentsystem.domain.payment.exception;

import com.commercepaymentsystem.global.exception.BusinessException;

public class PaymentException extends BusinessException {

	public PaymentException(PaymentErrorCode errorCode) {
		super(errorCode);
	}

	public PaymentException(PaymentErrorCode errorCode, String message) {
		super(errorCode, message);
	}

	public PaymentException(PaymentErrorCode errorCode, Throwable cause) {
		super(errorCode, cause);
	}
}
