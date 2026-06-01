package com.commercepaymentsystem.infrastructure.portone.exception;

public class PortOnePaymentVerificationException extends PortOneException {

	public PortOnePaymentVerificationException(String message) {
		super(message);
	}

	public PortOnePaymentVerificationException(String message, Throwable cause) {
		super(message, cause);
	}
}
