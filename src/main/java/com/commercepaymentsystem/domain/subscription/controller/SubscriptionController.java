package com.commercepaymentsystem.domain.subscription.controller;

import com.commercepaymentsystem.domain.subscription.dto.RegisterPaymentMethodRequest;
import com.commercepaymentsystem.domain.subscription.dto.StartSubscriptionRequest;
import com.commercepaymentsystem.domain.subscription.dto.SubscriptionResponse;
import com.commercepaymentsystem.domain.subscription.entity.PaymentMethod;
import com.commercepaymentsystem.domain.subscription.service.SubscriptionService;
import com.commercepaymentsystem.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

	private final SubscriptionService subscriptionService;

	/**
	 * 1. 결제 수단(빌링키) 등록
	 */
	@PostMapping("/payment-methods")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<Void> registerPaymentMethod(
		@AuthenticationPrincipal Long memberId,
		@Valid @RequestBody RegisterPaymentMethodRequest request
	) {
		subscriptionService.registerPaymentMethod(memberId, request);
		return ApiResponse.ok();
	}

	/**
	 * 2. 구독 시작 (첫 결제 즉시 진행)
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<SubscriptionResponse> startSubscription(
		@AuthenticationPrincipal Long memberId,
		@Valid @RequestBody StartSubscriptionRequest request
	) {
		return ApiResponse.ok(subscriptionService.startSubscription(memberId, request));
	}

	/**
	 * 3. 구독 해지
	 */
	@PostMapping("/cancel/{subscriptionId}")
	public ApiResponse<Void> cancelSubscription(
		@AuthenticationPrincipal Long memberId,
		@PathVariable Long subscriptionId
	) {
		subscriptionService.cancelSubscription(memberId, subscriptionId);
		return ApiResponse.ok();
	}

	/**
	 * 4. 내 구독 정보 조회
	 */
	@GetMapping("/me")
	public ApiResponse<SubscriptionResponse> getMySubscription(
		@AuthenticationPrincipal Long memberId
	) {
		return ApiResponse.ok(subscriptionService.getMySubscription(memberId));
	}
}
