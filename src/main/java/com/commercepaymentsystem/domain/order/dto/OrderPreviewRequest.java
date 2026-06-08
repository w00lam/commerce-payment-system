package com.commercepaymentsystem.domain.order.dto;

import java.util.List;

import jakarta.validation.constraints.Positive;

public record OrderPreviewRequest(

	List<@Positive(message = "장바구니 항목 ID는 양수여야 합니다.") Long> cartItemIds
) {
	public OrderPreviewRequest {
		if (cartItemIds == null) {
			cartItemIds = List.of();
		}
	}
}
