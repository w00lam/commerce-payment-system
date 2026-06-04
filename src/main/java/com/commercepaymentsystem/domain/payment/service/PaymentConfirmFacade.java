package com.commercepaymentsystem.domain.payment.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.commercepaymentsystem.domain.payment.dto.PaymentConfirmCommand;
import com.commercepaymentsystem.domain.payment.dto.PaymentConfirmResult;
import com.commercepaymentsystem.domain.payment.entity.Payment;

import lombok.RequiredArgsConstructor;

/**
 * 결제 승인과 승인 후처리(주문 확정, 포인트 차감 및 적립, 장바구니 정리)를 조율하는 파사드 클래스입니다.
 */
@Component
@RequiredArgsConstructor
public class PaymentConfirmFacade {

	private final PaymentService paymentService;
	private final PaymentPostProcessService paymentPostProcessService;

	/**
	 * 결제 확정을 진행하고 후처리를 연동하여 전체 결과를 반환합니다.
	 *
	 * @param command 결제 확정에 필요한 정보
	 * @return 확정된 결제 결과
	 */
	@Transactional
	public PaymentConfirmResult confirm(PaymentConfirmCommand command) {
		Payment payment = paymentService.confirmPayment(command);
		paymentPostProcessService.process(payment);
		return PaymentConfirmResult.from(payment);
	}
}
