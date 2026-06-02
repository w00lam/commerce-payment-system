package com.commercepaymentsystem.infrastructure.portone.exception;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClientResponseException;

public final class PortOneExceptionConverter {

	private PortOneExceptionConverter() {
	}

	public static PortOneException paymentException(RestClientResponseException exception, String operationName) {
		HttpStatusCode statusCode = exception.getStatusCode();

		if (isAuthenticationFailure(statusCode)) {
			return new PortOneException(
				"PortOne 인증 실패: " + operationName + " 요청 권한이 없습니다.",
				exception
			);
		}

		if (statusCode.is5xxServerError()) {
			return new PortOneRetryableException(
				"PortOne 재시도 가능 오류: " + operationName + " 요청에 실패했습니다.",
				exception
			);
		}

		return new PortOneException("PortOne " + operationName + " 실패", exception);
	}

	public static PortOneException cancelException(RestClientResponseException exception) {
		HttpStatusCode statusCode = exception.getStatusCode();
		String operationName = "결제 취소";

		if (isAuthenticationFailure(statusCode)) {
			return new PortOneException(
				"PortOne 인증 실패: " + operationName + " 요청 권한이 없습니다.",
				exception
			);
		}

		if (statusCode.is5xxServerError()) {
			return new PortOneRetryableException(
				"PortOne 재시도 가능 오류: " + operationName + " 요청에 실패했습니다.",
				exception
			);
		}

		PortOneErrorResponse errorResponse = parseErrorResponse(exception.getResponseBodyAsString());

		return new PortOneException(
			"PortOne 결제 취소 실패: " + resolveErrorMessage(errorResponse),
			errorResponse.type(),
			errorResponse.pgCode(),
			errorResponse.pgMessage(),
			exception
		);
	}

	private static PortOneErrorResponse parseErrorResponse(String responseBody) {
		return new PortOneErrorResponse(
			text(responseBody, "type"),
			text(responseBody, "message"),
			text(responseBody, "pgCode"),
			text(responseBody, "pgMessage")
		);
	}

	private static String resolveErrorMessage(PortOneErrorResponse errorResponse) {
		if (!isBlank(errorResponse.message())) {
			return errorResponse.message();
		}

		return "결제사 취소 요청이 거절되었습니다.";
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

	private record PortOneErrorResponse(
		String type,
		String message,
		String pgCode,
		String pgMessage
	) {
	}
}
