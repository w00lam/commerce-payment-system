package com.commercepaymentsystem.domain.refund.dto;

public record RefundItemCommand(
	Long orderItemId,
	Long quantity
) {
}
