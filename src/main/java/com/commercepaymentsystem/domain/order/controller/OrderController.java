package com.commercepaymentsystem.domain.order.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.commercepaymentsystem.domain.order.dto.OrderPreviewRequest;
import com.commercepaymentsystem.domain.order.dto.OrderPreviewResponse;
import com.commercepaymentsystem.domain.order.service.OrderService;
import com.commercepaymentsystem.global.exception.BusinessException;
import com.commercepaymentsystem.global.exception.GlobalErrorCode;
import com.commercepaymentsystem.global.jwt.JwtProvider;
import com.commercepaymentsystem.global.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

	private static final String BEARER_PREFIX = "Bearer ";

	private final OrderService orderService;
	private final JwtProvider jwtProvider;

	@PostMapping("/preview")
	public ApiResponse<OrderPreviewResponse> previewOrder(
		@RequestHeader("Authorization") String authorizationHeader,
		@Valid @RequestBody OrderPreviewRequest request
	) {
		String token = extractToken(authorizationHeader);
		Long memberId = jwtProvider.getMemberId(token);

		return ApiResponse.ok(orderService.previewOrder(memberId, request));
	}

	private String extractToken(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
		}

		return authorizationHeader.substring(BEARER_PREFIX.length());
	}
}