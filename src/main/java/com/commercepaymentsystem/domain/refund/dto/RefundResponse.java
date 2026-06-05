package com.commercepaymentsystem.domain.refund.dto;

import java.time.LocalDateTime;

import com.commercepaymentsystem.domain.refund.entity.RefundStatus;

public record RefundResponse(
	Long refundId,
	String paymentId,
	RefundStatus status,
	Long pointRefundAmount,
	Long pgRefundAmount,
	Long totalRefundAmount,
	LocalDateTime refundedAt
) {

	public static RefundResponse from(RefundResult result) {
		return new RefundResponse(
			result.refundId(),
			result.paymentId(),
			result.status(),
			result.pointRefundAmount(),
			result.pgRefundAmount(),
			result.totalRefundAmount(),
			result.refundedAt()
		);
	}
}
