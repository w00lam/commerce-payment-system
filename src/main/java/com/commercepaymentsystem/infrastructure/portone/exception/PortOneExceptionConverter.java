package com.commercepaymentsystem.infrastructure.portone.exception;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClientResponseException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public final class PortOneExceptionConverter {

	private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

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

	/**
	 * PortOne 오류 응답 본문을 JSON으로 파싱해 표준 오류 필드를 추출합니다.
	 *
	 * <p>본문이 비어 있거나 JSON이 아니면 원본 응답 본문은 보존하되 세부 필드는 비운 상태로
	 * 예외를 생성합니다.</p>
	 */
	private static PortOneErrorResponse parseErrorResponse(String responseBody) {
		if (isBlank(responseBody)) {
			return PortOneErrorResponse.empty();
		}

		try {
			JsonNode root = OBJECT_MAPPER.readTree(responseBody);
			return new PortOneErrorResponse(
				text(root, "type"),
				text(root, "message"),
				text(root, "pgCode"),
				text(root, "pgMessage")
			);
		} catch (Exception exception) {
			return PortOneErrorResponse.empty();
		}
	}

	private static String text(JsonNode node, String fieldName) {
		JsonNode value = node.path(fieldName);
		if (value.isMissingNode() || value.isNull()) {
			return null;
		}
		return value.asString();
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
		private static PortOneErrorResponse empty() {
			return new PortOneErrorResponse(null, null, null, null);
		}
	}
}
