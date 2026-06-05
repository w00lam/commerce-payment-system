package com.commercepaymentsystem.domain.member.exception;

import org.springframework.http.HttpStatus;

import com.commercepaymentsystem.global.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements ErrorCode {

	DUPLICATED_EMAIL(
		HttpStatus.CONFLICT,
		"MEMBER_001",
		"이미 가입된 이메일입니다."
	),

	INVALID_LOGIN_INFO(
		HttpStatus.UNAUTHORIZED,
		"MEMBER_002",
		"이메일 또는 비밀번호가 일치하지 않습니다."
	),

	DELETED_MEMBER(
		HttpStatus.FORBIDDEN,
		"MEMBER_003",
		"탈퇴한 회원입니다."
	),

	POINT_NOT_ENOUGH(
		HttpStatus.BAD_REQUEST,
		"MEMBER_004",
		"보유 포인트가 부족합니다."
	),

	MEMBER_NOT_FOUND(
		HttpStatus.NOT_FOUND,
		"MEMBER_005",
		"존재하지 않는 회원입니다."
	),

	INVALID_PASSWORD(
		HttpStatus.UNAUTHORIZED,
		"MEMBER_006",
		"비밀번호가 일치하지 않습니다."
	);

	private final HttpStatus status;
	private final String code;
	private final String message;
}
