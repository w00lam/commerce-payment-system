package com.commercepaymentsystem.domain.auth.dto;

import com.commercepaymentsystem.domain.member.entity.Member;

public record LoginResponse(
	String accessToken,
	String tokenType,
	MemberInfo member
) {

	private static final String TOKEN_TYPE = "Bearer";

	public static LoginResponse of(
		String accessToken,
		Member member
	) {
		return new LoginResponse(
			accessToken,
			TOKEN_TYPE,
			MemberInfo.from(member)
		);
	}

	public record MemberInfo(
		Long memberId,
		String email,
		String name
	) {

		public static MemberInfo from(Member member) {
			return new MemberInfo(
				member.getId(),
				member.getEmail(),
				member.getName()
			);
		}
	}
}