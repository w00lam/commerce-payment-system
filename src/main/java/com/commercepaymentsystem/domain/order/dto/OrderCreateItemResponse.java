package com.commercepaymentsystem.domain.order.dto;

public record OrderCreateItemResponse(

	Long orderItemId,
	Long productId,
	String productName,
	Long orderPrice,
	Long quantity,
	Long totalPrice
) {
}