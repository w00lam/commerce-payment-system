package com.commercepaymentsystem.domain.point.exception;

import com.commercepaymentsystem.global.exception.BusinessException;
import com.commercepaymentsystem.global.exception.ErrorCode;

public class PointException extends BusinessException {

	public PointException(ErrorCode errorCode) {
		super(errorCode);
	}

	public PointException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}
