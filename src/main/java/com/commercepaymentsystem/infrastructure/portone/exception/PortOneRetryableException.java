package com.commercepaymentsystem.infrastructure.portone.exception;

public class PortOneRetryableException extends PortOneException {

	public PortOneRetryableException(String message) {
		super(message);
	}

	public PortOneRetryableException(String message, Throwable cause) {
		super(message, cause);
	}
}
