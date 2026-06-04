package com.commercepaymentsystem.infrastructure.portone.exception;

public class PortOneRetryableException extends PortOneException {

	public PortOneRetryableException(String message) {
		super(message);
	}

	public PortOneRetryableException(String message, Throwable cause) {
		super(message, cause);
	}

	public PortOneRetryableException(
		String message,
		Integer statusCode,
		String errorType,
		String portOneMessage,
		String pgCode,
		String pgMessage,
		String responseBody,
		Throwable cause
	) {
		super(message, statusCode, errorType, portOneMessage, pgCode, pgMessage, responseBody, cause);
	}
}
