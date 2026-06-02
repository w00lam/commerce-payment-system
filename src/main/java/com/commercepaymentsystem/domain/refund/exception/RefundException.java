package com.commercepaymentsystem.domain.refund.exception;

import com.commercepaymentsystem.global.exception.BusinessException;

public class RefundException extends BusinessException {

	public RefundException(RefundErrorCode errorCode) {
		super(errorCode);
	}

	public RefundException(RefundErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}
