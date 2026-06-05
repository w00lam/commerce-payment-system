package com.commercepaymentsystem.domain.membership.exception;

import org.springframework.http.HttpStatus;

import com.commercepaymentsystem.global.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MembershipErrorCode implements ErrorCode {

	MEMBERSHIP_NOT_FOUND(
		HttpStatus.NOT_FOUND,
		"MEMBERSHIP_001",
		"멤버십 정보를 찾을 수 없습니다."
	),

	INVALID_GRADE_POLICY(
		HttpStatus.INTERNAL_SERVER_ERROR,
		"MEMBERSHIP_002",
		"멤버십 등급 정책이 올바르지 않습니다."
	),

	INVALID_CUMULATIVE_PAYMENT_AMOUNT(
		HttpStatus.BAD_REQUEST,
		"MEMBERSHIP_003",
		"누적 결제 금액은 음수가 될 수 없습니다."
	);

	private final HttpStatus status;
	private final String code;
	private final String message;
}