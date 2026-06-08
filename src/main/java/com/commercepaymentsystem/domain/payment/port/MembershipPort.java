package com.commercepaymentsystem.domain.payment.port;

public interface MembershipPort {

	void applyPayment(Long memberId, Long paidAmount);
}
