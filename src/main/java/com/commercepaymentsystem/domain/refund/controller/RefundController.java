package com.commercepaymentsystem.domain.refund.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.commercepaymentsystem.domain.refund.dto.RefundRequest;
import com.commercepaymentsystem.domain.refund.dto.RefundResponse;
import com.commercepaymentsystem.domain.refund.dto.RefundResult;
import com.commercepaymentsystem.domain.refund.facade.RefundFacade;
import com.commercepaymentsystem.global.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/payments/{paymentId}/refunds")
@RequiredArgsConstructor
public class RefundController {

	private final RefundFacade refundFacade;

	@PostMapping
	public ApiResponse<RefundResponse> refundPayment(
		@PathVariable String paymentId,
		@AuthenticationPrincipal Long memberId,
		@Valid @RequestBody RefundRequest request
	) {
		RefundResult result = refundFacade.refundPayment(request.toCommand(paymentId, memberId));
		return ApiResponse.ok(RefundResponse.from(result));
	}
}
