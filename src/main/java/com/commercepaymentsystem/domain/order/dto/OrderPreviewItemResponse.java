package com.commercepaymentsystem.domain.order.dto;

public record OrderPreviewItemResponse(

	Long cartItemId,
	Long productId,
	String productName,
	Long currentPrice,
	Long quantity,
	Long totalPrice
) {
}