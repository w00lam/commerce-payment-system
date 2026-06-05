package com.commercepaymentsystem.domain.payment.adapter;

import org.springframework.stereotype.Component;

import com.commercepaymentsystem.domain.payment.port.PointPort;
import com.commercepaymentsystem.domain.point.service.PointService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentPointAdapter implements PointPort {

	private final PointService pointService;

	@Override
	public void deductUsedPoint(Long memberId, Long amount, Long paymentId) {
		pointService.deductPoint(memberId, amount, paymentId);
	}

	@Override
	public void earnPoint(Long memberId, Long amount, Long paymentId) {
		pointService.earnPoint(memberId, amount, paymentId);
	}
}
