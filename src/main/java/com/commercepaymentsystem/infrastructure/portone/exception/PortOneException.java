package com.commercepaymentsystem.infrastructure.portone.exception;

public class PortOneException extends RuntimeException {

	private final Integer statusCode;
	private final String errorType;
	private final String portOneMessage;
	private final String pgCode;
	private final String pgMessage;
	private final String responseBody;

	public PortOneException(String message) {
		this(message, null, null, null, null, null, null, null);
	}

	public PortOneException(String message, Throwable cause) {
		this(message, null, null, null, null, null, null, cause);
	}

	public PortOneException(
		String message,
		String errorType,
		String pgCode,
		String pgMessage,
		Throwable cause
	) {
		this(message, null, errorType, null, pgCode, pgMessage, null, cause);
	}

	public PortOneException(
		String message,
		Integer statusCode,
		String errorType,
		String portOneMessage,
		String pgCode,
		String pgMessage,
		String responseBody,
		Throwable cause
	) {
		super(message, cause);
		this.statusCode = statusCode;
		this.errorType = errorType;
		this.portOneMessage = portOneMessage;
		this.pgCode = pgCode;
		this.pgMessage = pgMessage;
		this.responseBody = responseBody;
	}

	public Integer getStatusCode() {
		return statusCode;
	}

	public String getErrorType() {
		return errorType;
	}

	public String getPortOneMessage() {
		return portOneMessage;
	}

	public String getPgCode() {
		return pgCode;
	}

	public String getPgMessage() {
		return pgMessage;
	}

	public String getResponseBody() {
		return responseBody;
	}
}
