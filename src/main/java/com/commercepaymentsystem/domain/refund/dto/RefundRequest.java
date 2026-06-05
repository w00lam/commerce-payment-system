package com.commercepaymentsystem.domain.refund.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record RefundRequest(
	@NotBlank
	String reason,

	@Valid
	@NotEmpty
	List<RefundItemRequest> items
) {

	public RefundCommand toCommand(String paymentId, Long memberId) {
		return new RefundCommand(
			paymentId,
			memberId,
			reason,
			items.stream()
				.map(RefundItemRequest::toCommand)
				.toList()
		);
	}
}
