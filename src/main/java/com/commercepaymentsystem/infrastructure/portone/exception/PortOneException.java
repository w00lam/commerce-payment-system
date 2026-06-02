package com.commercepaymentsystem.infrastructure.portone.exception;

public class PortOneException extends RuntimeException {

	private final String errorType;
	private final String pgCode;
	private final String pgMessage;

	public PortOneException(String message) {
		this(message, null, null, null, null);
	}

	public PortOneException(String message, Throwable cause) {
		this(message, null, null, null, cause);
	}

	public PortOneException(
		String message,
		String errorType,
		String pgCode,
		String pgMessage,
		Throwable cause
	) {
		super(message, cause);
		this.errorType = errorType;
		this.pgCode = pgCode;
		this.pgMessage = pgMessage;
	}

	public String getErrorType() {
		return errorType;
	}

	public String getPgCode() {
		return pgCode;
	}

	public String getPgMessage() {
		return pgMessage;
	}
}
