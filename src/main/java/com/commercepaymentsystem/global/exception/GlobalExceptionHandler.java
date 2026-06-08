package com.commercepaymentsystem.global.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.commercepaymentsystem.global.response.ApiResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
		ErrorCode code = e.getErrorCode();
		return ResponseEntity.status(code.getStatus())
			.body(ApiResponse.error(code, e.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
		String message = e.getBindingResult().getFieldErrors().stream()
			.map(err -> err.getField() + ": " + err.getDefaultMessage())
			.reduce((a, b) -> a + ", " + b)
			.orElse("입력값이 올바르지 않습니다");
		return ResponseEntity.badRequest()
			.body(ApiResponse.error(GlobalErrorCode.INVALID_INPUT_VALUE, message));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResponse<Void>> handleMessageNotReadable(HttpMessageNotReadableException e) {
		return ResponseEntity.badRequest()
			.body(ApiResponse.error(GlobalErrorCode.INVALID_INPUT_VALUE, "요청 본문을 읽을 수 없습니다."));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
		return ResponseEntity.badRequest()
			.body(ApiResponse.error(GlobalErrorCode.INVALID_INPUT_VALUE, "요청 파라미터 타입이 올바르지 않습니다."));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
		log.error("서버 오류 발생", e);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(ApiResponse.error(GlobalErrorCode.INTERNAL_SERVER_ERROR));
	}
}
