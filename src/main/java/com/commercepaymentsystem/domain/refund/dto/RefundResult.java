package com.commercepaymentsystem.domain.refund.dto;

import java.time.LocalDateTime;

import com.commercepaymentsystem.domain.refund.entity.Refund;
import com.commercepaymentsystem.domain.refund.entity.RefundStatus;

public record RefundResult(
	Long refundId,
	String paymentId,
	RefundStatus status,
	Long pointRefundAmount,
	Long pgRefundAmount,
	Long totalRefundAmount,
	LocalDateTime refundedAt
) {

	public static RefundResult from(Refund refund, String paymentId) {
		return new RefundResult(
			refund.getId(),
			paymentId,
			refund.getStatus(),
			refund.getPointRefundAmount(),
			refund.getPgRefundAmount(),
			refund.getTotalRefundAmount(),
			refund.getUpdatedAt()
		);
	}
}
