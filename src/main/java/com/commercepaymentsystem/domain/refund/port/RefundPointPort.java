package com.commercepaymentsystem.domain.refund.port;

public interface RefundPointPort {

	void restorePoint(Long memberId, Long amount, Long paymentId, Long refundId);

	void revokeEarnedPoint(Long memberId, Long amount, Long paymentId, Long refundId);

	long getRevokedEarnedPointAmount(Long paymentId);
}
