package com.commercepaymentsystem.domain.order.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.commercepaymentsystem.domain.order.dto.GetOrderDetailResponse;
import com.commercepaymentsystem.domain.order.dto.GetOrderResponse;
import com.commercepaymentsystem.domain.order.dto.OrderCreateRequest;
import com.commercepaymentsystem.domain.order.dto.OrderCreateResponse;
import com.commercepaymentsystem.domain.order.dto.OrderPreviewRequest;
import com.commercepaymentsystem.domain.order.dto.OrderPreviewResponse;
import com.commercepaymentsystem.domain.order.service.OrderFacade;
import com.commercepaymentsystem.domain.order.service.OrderService;
import com.commercepaymentsystem.global.response.ApiResponse;
import com.commercepaymentsystem.global.response.PageResponse;

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
		return ApiResponse.ok(orderFacade.previewOrder(memberId, request));
	}

	// 내 주문 목록 조회
	@GetMapping
	public ApiResponse<PageResponse<GetOrderResponse>> getOrders(
		@AuthenticationPrincipal Long memberId,
		@PageableDefault(size = 20, sort = "createdAt",
			direction = Sort.Direction.DESC) Pageable pageable
	) {
		PageResponse<GetOrderResponse> response = orderService.getOrders(memberId, pageable);
		return ApiResponse.ok(response);
	}

	// 내 주문 단건 상세 조회
	@GetMapping("/{orderId}")
	public ApiResponse<GetOrderDetailResponse> getMyOrderDetail(
		@AuthenticationPrincipal Long memberId, @PathVariable Long orderId
	) {
		return ApiResponse.ok(orderFacade.getOrderDetail(memberId, orderId));
	}
}