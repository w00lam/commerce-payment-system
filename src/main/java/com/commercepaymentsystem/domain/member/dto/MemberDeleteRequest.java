package com.commercepaymentsystem.domain.member.dto;

import jakarta.validation.constraints.NotBlank;

public record MemberDeleteRequest(

	@NotBlank(message = "비밀번호는 필수입니다.")
	String password
) {
}
