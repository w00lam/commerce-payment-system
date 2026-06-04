package com.commercepaymentsystem.domain.order.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.commercepaymentsystem.domain.order.entity.Order;
import com.commercepaymentsystem.domain.order.entity.OrderStatus;
import com.commercepaymentsystem.domain.payment.dto.PaymentCreateResult;

import jakarta.annotation.Nullable;

public record GetOrderDetailResponse(
	Long orderId,
	String orderNumber,
	String orderName,
	OrderStatus orderStatus,
	Long totalPrice,
	Long usedPointAmount,
	LocalDateTime orderedAt,
	List<OrderItemCreateResponse> orderItems,
	PaymentCreateResult payment
) {

	public static GetOrderDetailResponse of(
		Order order,
		@Nullable PaymentCreateResult payment
	) {
		return new GetOrderDetailResponse(
			order.getId(),
			order.getOrderNumber(),
			order.getOrderName(),
			order.getStatus(),
			order.getTotalPrice(),
			order.getUsedPointAmount(),
			order.getCreatedAt(),
			order.getOrderItems().stream()
				.map(OrderItemCreateResponse::from)
				.toList(),
			payment
		);
	}
}