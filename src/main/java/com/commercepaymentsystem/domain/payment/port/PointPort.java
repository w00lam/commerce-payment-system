package com.commercepaymentsystem.domain.payment.port;

public interface PointPort {

	void deductUsedPoint(Long memberId, Long amount, Long paymentId);

	void earnPoint(Long memberId, Long amount, Long paymentId);
}
