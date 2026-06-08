package com.commercepaymentsystem.domain.payment.adapter;

import org.springframework.stereotype.Component;

import com.commercepaymentsystem.domain.membership.service.MembershipService;
import com.commercepaymentsystem.domain.payment.port.MembershipPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentMembershipAdapter implements MembershipPort {

	private final MembershipService membershipService;

	@Override
	public void applyPayment(Long memberId, Long paidAmount) {
		membershipService.applyPayment(memberId, paidAmount);
	}
}
