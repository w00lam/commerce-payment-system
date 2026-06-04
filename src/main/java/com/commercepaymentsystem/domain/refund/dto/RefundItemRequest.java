package com.commercepaymentsystem.domain.refund.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RefundItemRequest(
	@NotNull
	@Positive
	Long orderItemId,

	@NotNull
	@Positive
	Long quantity
) {

	public RefundItemCommand toCommand() {
		return new RefundItemCommand(orderItemId, quantity);
	}
}
