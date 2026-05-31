package com.commercepaymentsystem.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(

	@NotBlank(message = "이메일은 필수입니다.")
	@Email(message = "이메일 형식이 올바르지 않습니다.")
	@Size(max = 100, message = "이메일은 100자 이하로 입력해주세요.")
	String email,

	@NotBlank(message = "비밀번호는 필수입니다.")
	@Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하로 입력해주세요.")
	String password,

	@NotBlank(message = "이름은 필수입니다.")
	@Size(min = 2, max = 50, message = "이름은 2자 이상 50자 이하로 입력해주세요.")
	String name,

	@NotBlank(message = "전화번호는 필수입니다.")
	@Pattern(
		regexp = "^010-\\d{4}-\\d{4}$",
		message = "전화번호는 010-0000-0000 형식이어야 합니다."
	)
	String phone
) {
}