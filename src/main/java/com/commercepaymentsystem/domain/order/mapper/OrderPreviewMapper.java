package com.commercepaymentsystem.domain.order.mapper;

import java.util.List;

import com.commercepaymentsystem.domain.cart.entity.CartItem;
import com.commercepaymentsystem.domain.order.dto.OrderPreviewItemResponse;
import com.commercepaymentsystem.domain.order.dto.OrderPreviewResponse;

public class OrderPreviewMapper {

	private OrderPreviewMapper() {
	}

	public static OrderPreviewItemResponse toItemResponse(CartItem cartItem) {
		Long currentPrice = cartItem.getProduct().getPrice();
		Long quantity = cartItem.getQuantity();

		return new OrderPreviewItemResponse(
			cartItem.getId(),
			cartItem.getProduct().getId(),
			cartItem.getProduct().getName(),
			currentPrice,
			quantity,
			currentPrice * quantity
		);
	}

	public static OrderPreviewResponse toResponse(
		Long memberId,
		Long totalAmount,
		List<OrderPreviewItemResponse> items
	) {
		return new OrderPreviewResponse(
			memberId,
			totalAmount,
			items
		);
	}
}