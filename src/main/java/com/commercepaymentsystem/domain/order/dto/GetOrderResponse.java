package com.commercepaymentsystem.domain.order.dto;

import java.time.LocalDateTime;

import com.commercepaymentsystem.domain.order.entity.Order;
import com.commercepaymentsystem.domain.order.entity.OrderStatus;

public record GetOrderResponse(
	Long orderId,
	String orderNumber,
	OrderStatus status,
	Long totalAmount,
	Long usedPointAmount,
	LocalDateTime orderedAt
) {

	public static GetOrderResponse from(Order order) {
		return new GetOrderResponse(
			order.getId(),
			order.getOrderNumber(),
			order.getStatus(),
			order.getTotalPrice(),
			order.getUsedPointAmount(),
			order.getCreatedAt()
		);
	}
}