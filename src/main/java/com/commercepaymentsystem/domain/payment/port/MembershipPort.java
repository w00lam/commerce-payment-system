package com.commercepaymentsystem.domain.payment.port;

public interface MembershipPort {

	int getPointRewardRate(Long memberId);

	void applyPayment(Long memberId, Long paidAmount);
}
