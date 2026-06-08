package com.commercepaymentsystem.domain.refund.port;

public interface RefundMembershipPort {

	void applyRefund(Long memberId, Long refundAmount);
}
