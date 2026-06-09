package com.commercepaymentsystem.domain.membership.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.commercepaymentsystem.domain.membership.dto.MembershipGradeResponse;
import com.commercepaymentsystem.domain.membership.dto.MembershipRecalculateResponse;
import com.commercepaymentsystem.domain.membership.dto.MembershipResponse;
import com.commercepaymentsystem.domain.membership.service.MembershipService;
import com.commercepaymentsystem.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/memberships")
@RequiredArgsConstructor
public class MembershipController {

	private final MembershipService membershipService;

	@GetMapping("/me")
	public ApiResponse<MembershipResponse> getMyMembership(
		@AuthenticationPrincipal Long memberId
	) {
		return ApiResponse.ok(membershipService.getMyMembership(memberId));
	}

	@GetMapping("/grades")
	public ApiResponse<List<MembershipGradeResponse>> getGrades() {
		return ApiResponse.ok(membershipService.getGrades());
	}

	@PostMapping("/recalculate")
	public ApiResponse<MembershipRecalculateResponse> recalculate(
		@AuthenticationPrincipal Long memberId
	) {
		return ApiResponse.ok(membershipService.recalculate(memberId));
	}
}