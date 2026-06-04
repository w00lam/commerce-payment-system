package com.commercepaymentsystem.domain.payment.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.commercepaymentsystem.domain.payment.dto.PaymentConfirmCommand;
import com.commercepaymentsystem.domain.payment.dto.PaymentConfirmResult;
import com.commercepaymentsystem.domain.payment.service.PaymentConfirmFacade;
import com.commercepaymentsystem.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

	private final PaymentConfirmFacade paymentConfirmFacade;

	@PostMapping("/{paymentId}/confirm")
	public ApiResponse<PaymentConfirmResult> confirmPayment(
		@PathVariable String paymentId,
		@AuthenticationPrincipal Long memberId
	) {
		PaymentConfirmCommand command = PaymentConfirmCommand.of(paymentId, memberId);
		PaymentConfirmResult response = paymentConfirmFacade.confirm(command);

		return ApiResponse.ok(response);
	}
}
