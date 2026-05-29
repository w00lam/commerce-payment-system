package com.commercepaymentsystem.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.commercepaymentsystem.global.exception.ErrorCode;

import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

	private static final String SUCCESS_CODE = "SUCCESS";

	private final String code;
	private final String message;
	private final T data;

	private ApiResponse(String code, String message, T data) {
		this.code = code;
		this.message = message;
		this.data = data;
	}

	public static <T> ApiResponse<T> ok(T data) {
		return new ApiResponse<>(SUCCESS_CODE, null, data);
	}

	public static ApiResponse<Void> ok() {
		return new ApiResponse<>(SUCCESS_CODE, null, null);
	}

	public static ApiResponse<Void> error(ErrorCode errorCode) {
		return new ApiResponse<>(errorCode.getCode(), errorCode.getMessage(), null);
	}

	public static ApiResponse<Void> error(ErrorCode errorCode, String message) {
		return new ApiResponse<>(errorCode.getCode(), message, null);
	}

	public static ApiResponse<Void> error(String code, String message) {
		return new ApiResponse<>(code, message, null);
	}
}