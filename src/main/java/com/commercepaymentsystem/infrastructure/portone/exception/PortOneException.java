package com.commercepaymentsystem.infrastructure.portone.exception;

public abstract class PortOneException extends RuntimeException {

	protected PortOneException(String message) {
		super(message);
	}

	protected PortOneException(String message, Throwable cause) {
		super(message, cause);
	}
}
