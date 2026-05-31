package com.commercepaymentsystem.domain.point.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.commercepaymentsystem.domain.point.dto.PointHistoryResponse;
import com.commercepaymentsystem.domain.point.dto.PointResponse;
import com.commercepaymentsystem.domain.point.service.PointService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointController {

	private final PointService pointService;

	@GetMapping
	public ResponseEntity<PointResponse> getMyPoint(@AuthenticationPrincipal Long memberId) {
		return ResponseEntity.ok(pointService.getMyPoint(memberId));
	}

	@GetMapping("/histories")
	public ResponseEntity<List<PointHistoryResponse>> getMyPointHistories(@AuthenticationPrincipal Long memberId) {
		return ResponseEntity.ok(pointService.getMyPointHistories(memberId));
	}
}