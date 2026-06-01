package com.commercepaymentsystem.domain.point.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.commercepaymentsystem.domain.point.dto.PointDeductRequest;
import com.commercepaymentsystem.domain.point.dto.PointHistoryResponse;
import com.commercepaymentsystem.domain.point.dto.PointResponse;
import com.commercepaymentsystem.domain.point.service.PointService;
import com.commercepaymentsystem.global.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointController {

	private final PointService pointService;

	@GetMapping
	public ApiResponse<PointResponse> getMyPoint(@AuthenticationPrincipal Long memberId) {
		return ApiResponse.ok(pointService.getMyPoint(memberId));
	}

	@GetMapping("/histories")
	public ApiResponse<Page<PointHistoryResponse>> getMyPointHistories(
		@AuthenticationPrincipal Long memberId,
		@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
	) {
		return ApiResponse.ok(pointService.getMyPointHistories(memberId, pageable));
	}

	@PostMapping("/deduct")
	public ApiResponse<Void> deductPoint(
		@AuthenticationPrincipal Long memberId,
		@RequestBody @Valid PointDeductRequest request
	) {
		pointService.deductPoint(memberId, request.amount(), request.paymentId());
		return ApiResponse.ok();
	}
}
