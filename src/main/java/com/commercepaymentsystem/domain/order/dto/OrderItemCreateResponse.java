package com.commercepaymentsystem.domain.order.dto;

import com.commercepaymentsystem.domain.order.entity.OrderItem;

public record OrderItemCreateResponse(

	Long orderItemId,
	Long productId,
	String productName,
	Long orderPrice,
	Long quantity,
	Long totalPrice
) {
	public static OrderItemCreateResponse from(OrderItem orderItem) {
		return new OrderItemCreateResponse(
			orderItem.getId(),
			orderItem.getProductId(),
			orderItem.getProductName(),
			orderItem.getOrderPrice(),
			orderItem.getQuantity(),
			orderItem.getOrderPrice() * orderItem.getQuantity()
		);
	}
}