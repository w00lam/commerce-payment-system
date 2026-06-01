package com.commercepaymentsystem.infrastructure.portone.exception;

public class PortOneAuthenticationException extends PortOneException {

	public PortOneAuthenticationException(String message) {
		super(message);
	}

	public PortOneAuthenticationException(String message, Throwable cause) {
		super(message, cause);
	}
}
