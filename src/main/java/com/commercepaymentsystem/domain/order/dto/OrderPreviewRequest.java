package com.commercepaymentsystem.domain.order.dto;

import java.util.List;

public record OrderPreviewRequest(

	List<Long> cartItemIds
) {
	public OrderPreviewRequest {
		if (cartItemIds == null) {
			cartItemIds = List.of();
		}
	}
}