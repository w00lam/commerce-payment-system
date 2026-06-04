package com.commercepaymentsystem.domain.refund.dto;

import java.util.List;

public record RefundCommand(
	String paymentId,
	Long memberId,
	String reason,
	List<RefundItemCommand> items
) {
}
