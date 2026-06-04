package com.commercepaymentsystem.domain.refund.adapter;

import org.springframework.stereotype.Component;

import com.commercepaymentsystem.domain.point.service.PointService;
import com.commercepaymentsystem.domain.refund.port.RefundPointPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RefundPointAdapter implements RefundPointPort {

	private final PointService pointService;

	@Override
	public void restorePoint(Long memberId, Long amount, Long paymentId, Long refundId) {
		pointService.restorePoint(memberId, amount, paymentId, refundId);
	}

	@Override
	public void revokeEarnedPoint(Long memberId, Long amount, Long paymentId, Long refundId) {
		pointService.revokeEarnedPoint(memberId, amount, paymentId, refundId);
	}

	@Override
	public long getRevokedEarnedPointAmount(Long paymentId) {
		return pointService.getRevokedEarnedPointAmount(paymentId);
	}
}
