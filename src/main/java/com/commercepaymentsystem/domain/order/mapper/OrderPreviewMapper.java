package com.commercepaymentsystem.domain.order.mapper;

import java.util.List;

import com.commercepaymentsystem.domain.cart.entity.CartItem;
import com.commercepaymentsystem.domain.order.dto.OrderPreviewItemResponse;
import com.commercepaymentsystem.domain.order.dto.OrderPreviewResponse;
import com.commercepaymentsystem.domain.product.entity.Product;

public class OrderPreviewMapper {

	private OrderPreviewMapper() {
	}

	public static OrderPreviewItemResponse toItemResponse(
		CartItem cartItem,
		Product product
	) {
		Long currentPrice = product.getPrice();
		Long quantity = cartItem.getQuantity();

		return new OrderPreviewItemResponse(
			cartItem.getId(),
			product.getId(),
			product.getName(),
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