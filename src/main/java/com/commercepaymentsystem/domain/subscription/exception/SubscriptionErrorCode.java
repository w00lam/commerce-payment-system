package com.commercepaymentsystem.domain.subscription.exception;

import com.commercepaymentsystem.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SubscriptionErrorCode implements ErrorCode {

	PAYMENT_METHOD_NOT_FOUND(
		HttpStatus.NOT_FOUND,
		"SUB_001",
		"존재하지 않는 결제 수단입니다."
	),

	PLAN_NOT_FOUND(
		HttpStatus.NOT_FOUND,
		"SUB_002",
		"존재하지 않는 요금제입니다."
	),

	SUBSCRIPTION_NOT_FOUND(
		HttpStatus.NOT_FOUND,
		"SUB_003",
		"구독 정보를 찾을 수 없습니다."
	),

	DUPLICATE_SUBSCRIPTION(
		HttpStatus.CONFLICT,
		"SUB_004",
		"이미 활성화된 구독이 존재합니다."
	),

	INVALID_SUBSCRIPTION_STATUS(
		HttpStatus.BAD_REQUEST,
		"SUB_005",
		"변경할 수 없는 구독 상태입니다."
	),

	FIRST_PAYMENT_FAILED(
		HttpStatus.BAD_REQUEST,
		"SUB_006",
		"구독 시작 결제에 실패하여 구독을 진행할 수 없습니다."
	);

	private final HttpStatus status;
	private final String code;
	private final String message;
}
