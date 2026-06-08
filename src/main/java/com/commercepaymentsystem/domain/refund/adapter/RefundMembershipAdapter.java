package com.commercepaymentsystem.domain.refund.adapter;

import org.springframework.stereotype.Component;

import com.commercepaymentsystem.domain.membership.service.MembershipService;
import com.commercepaymentsystem.domain.refund.port.RefundMembershipPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RefundMembershipAdapter implements RefundMembershipPort {

	private final MembershipService membershipService;

	@Override
	public void applyRefund(Long memberId, Long refundAmount) {
		membershipService.applyRefund(memberId, refundAmount);
	}
}
