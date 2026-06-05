package com.commercepaymentsystem.infrastructure.portone.exception;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClientResponseException;

public final class PortOneExceptionConverter {

	private PortOneExceptionConverter() {
	}

	public static PortOneException paymentException(RestClientResponseException exception, String operationName) {
		return convert(exception, operationName);
	}

	public static PortOneException cancelException(RestClientResponseException exception) {
		return convert(exception, "결제 취소");
	}

	private static PortOneException convert(RestClientResponseException exception, String operationName) {
		HttpStatusCode statusCode = exception.getStatusCode();
		String responseBody = exception.getResponseBodyAsString();
		PortOneErrorResponse errorResponse = parseErrorResponse(responseBody);

		if (statusCode.is5xxServerError()) {
			return new PortOneRetryableException(
				detailMessage(operationName, "재시도 가능 오류", statusCode, errorResponse, responseBody),
				statusCode.value(),
				errorResponse.type(),
				errorResponse.message(),
				errorResponse.pgCode(),
				errorResponse.pgMessage(),
				responseBody,
				exception
			);
		}

		String reason = isAuthenticationFailure(statusCode) ? "인증 실패" : "실패";
		return new PortOneException(
			detailMessage(operationName, reason, statusCode, errorResponse, responseBody),
			statusCode.value(),
			errorResponse.type(),
			errorResponse.message(),
			errorResponse.pgCode(),
			errorResponse.pgMessage(),
			responseBody,
			exception
		);
	}

	private static String detailMessage(
		String operationName,
		String reason,
		HttpStatusCode statusCode,
		PortOneErrorResponse errorResponse,
		String responseBody
	) {
		return messagePrefix(operationName, reason)
			+ ". status=" + statusCode.value()
			+ ", type=" + valueOrDash(errorResponse.type())
			+ ", message=" + valueOrDash(errorResponse.message())
			+ ", pgCode=" + valueOrDash(errorResponse.pgCode())
			+ ", pgMessage=" + valueOrDash(errorResponse.pgMessage())
			+ ", responseBody=" + valueOrDash(responseBody);
	}

	private static String messagePrefix(String operationName, String reason) {
		if ("인증 실패".equals(reason)) {
			return "PortOne 인증 실패: " + operationName;
		}

		if ("재시도 가능 오류".equals(reason)) {
			return "PortOne 재시도 가능 오류: " + operationName;
		}

		return "PortOne " + operationName + " " + reason;
	}

	private static PortOneErrorResponse parseErrorResponse(String responseBody) {
		return new PortOneErrorResponse(
			text(responseBody, "type"),
			text(responseBody, "message"),
			text(responseBody, "pgCode"),
			text(responseBody, "pgMessage")
		);
	}

	private static String text(String responseBody, String fieldName) {
		if (isBlank(responseBody)) {
			return null;
		}

		String fieldPattern = "\"" + fieldName + "\"";
		int fieldIndex = responseBody.indexOf(fieldPattern);

		if (fieldIndex < 0) {
			return null;
		}

		int colonIndex = responseBody.indexOf(':', fieldIndex + fieldPattern.length());

		if (colonIndex < 0) {
			return null;
		}

		int valueStartIndex = responseBody.indexOf('"', colonIndex + 1);

		if (valueStartIndex < 0) {
			return null;
		}

		int valueEndIndex = responseBody.indexOf('"', valueStartIndex + 1);

		if (valueEndIndex < 0) {
			return null;
		}

		return responseBody.substring(valueStartIndex + 1, valueEndIndex);
	}

	private static boolean isAuthenticationFailure(HttpStatusCode statusCode) {
		return statusCode.value() == 401 || statusCode.value() == 403;
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private static String valueOrDash(String value) {
		if (isBlank(value)) {
			return "-";
		}

		return value;
	}

	private record PortOneErrorResponse(
		String type,
		String message,
		String pgCode,
		String pgMessage
	) {
	}
}
