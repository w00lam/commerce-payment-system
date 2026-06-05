package com.commercepaymentsystem.domain.auth.dto;

import com.commercepaymentsystem.domain.member.entity.Member;

public record SignupResponse(
	Long memberId,
	String email,
	String name,
	String phone,
	Long pointBalance
) {

	public static SignupResponse from(Member member) {
		return new SignupResponse(
			member.getId(),
			member.getEmail(),
			member.getName(),
			member.getPhone(),
			member.getPointBalance()
		);
	}
}