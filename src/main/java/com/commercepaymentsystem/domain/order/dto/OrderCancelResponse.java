package com.commercepaymentsystem.domain.order.dto;

import com.commercepaymentsystem.domain.order.entity.Order;
import com.commercepaymentsystem.domain.order.entity.OrderStatus;

public record OrderCancelResponse(
	Long orderId,
	String orderNumber,
	OrderStatus status
) {

	public static OrderCancelResponse from(Order order) {
		return new OrderCancelResponse(
			order.getId(),
			order.getOrderNumber(),
			order.getStatus()
		);
	}
}