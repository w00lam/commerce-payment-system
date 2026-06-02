package com.commercepaymentsystem.domain.member.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.commercepaymentsystem.domain.member.dto.MemberDeleteRequest;
import com.commercepaymentsystem.domain.member.service.MemberService;
import com.commercepaymentsystem.global.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

	private final MemberService memberService;

	@DeleteMapping("/me")
	public ApiResponse<Void> deleteMyAccount(
		@AuthenticationPrincipal Long memberId,
		@Valid @RequestBody MemberDeleteRequest request
	) {
		memberService.deleteMyAccount(
			memberId,
			request
		);

		return ApiResponse.ok();
	}
}
