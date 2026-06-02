package com.commercepaymentsystem.domain.order.mapper;

import java.util.List;

import com.commercepaymentsystem.domain.order.dto.OrderCreateItemResponse;
import com.commercepaymentsystem.domain.order.dto.OrderCreateResponse;
import com.commercepaymentsystem.domain.order.entity.Order;
import com.commercepaymentsystem.domain.order.entity.OrderItem;
import com.commercepaymentsystem.domain.payment.dto.PaymentCreateResult;

public final class OrderCreateMapper {

	private OrderCreateMapper() {
	}

	public static OrderCreateResponse toResponse(
		Order order,
		PaymentCreateResult payment,
		List<OrderItem> orderItems
	) {
		return new OrderCreateResponse(
			order.getId(),
			order.getOrderNumber(),
			order.getMember().getId(),
			order.getTotalAmount(),
			order.getUsedPointAmount(),
			payment.finalPaymentAmount(),
			order.getStatus(),
			payment.paymentId(),
			payment.status(),
			orderItems.stream()
				.map(OrderCreateMapper::toItemResponse)
				.toList()
		);
	}

	private static OrderCreateItemResponse toItemResponse(OrderItem orderItem) {
		return new OrderCreateItemResponse(
			orderItem.getId(),
			orderItem.getProductId(),
			orderItem.getProductName(),
			orderItem.getOrderPrice(),
			orderItem.getQuantity(),
			orderItem.getTotalPrice()
		);
	}
}