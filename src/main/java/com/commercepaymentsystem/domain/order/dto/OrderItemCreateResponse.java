package com.commercepaymentsystem.domain.order.dto;

public record OrderItemCreateResponse(

	Long orderItemId,
	Long productId,
	String productName,
	Long orderPrice,
	Long quantity,
	Long totalPrice
) {
}