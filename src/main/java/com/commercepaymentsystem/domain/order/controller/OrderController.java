package com.commercepaymentsystem.domain.order.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.commercepaymentsystem.domain.order.dto.OrderCreateRequest;
import com.commercepaymentsystem.domain.order.dto.OrderCreateResponse;
import com.commercepaymentsystem.domain.order.dto.OrderPreviewRequest;
import com.commercepaymentsystem.domain.order.dto.OrderPreviewResponse;
import com.commercepaymentsystem.domain.order.service.OrderFacade;
import com.commercepaymentsystem.domain.order.service.OrderService;
import com.commercepaymentsystem.global.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

	private final OrderFacade orderFacade;
	private final OrderService orderService;

	@PostMapping
	public ApiResponse<OrderCreateResponse> createOrder(
		@AuthenticationPrincipal Long memberId,
		@Valid @RequestBody OrderCreateRequest request
	) {
		return ApiResponse.ok(orderFacade.createOrder(memberId, request));
	}

	@PostMapping("/preview")
	public ApiResponse<OrderPreviewResponse> previewOrder(
		@AuthenticationPrincipal Long memberId,
		@Valid @RequestBody OrderPreviewRequest request
	) {
		return ApiResponse.ok(orderService.previewOrder(memberId, request));
	}
}