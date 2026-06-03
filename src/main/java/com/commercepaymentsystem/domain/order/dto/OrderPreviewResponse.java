package com.commercepaymentsystem.domain.order.dto;

import java.util.List;

public record OrderPreviewResponse(

	Long memberId,
	Long totalAmount,
	List<CheckoutItemResponse> items
) {
	public record CheckoutItemResponse(
		Long productId,
		String productName,
		Long price,
		Long quantity,
		long subtotal
	) {}
}