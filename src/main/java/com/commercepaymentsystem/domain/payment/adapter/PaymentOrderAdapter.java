package com.commercepaymentsystem.domain.payment.adapter;

import java.util.List;

import org.springframework.stereotype.Component;

import com.commercepaymentsystem.domain.order.entity.Order;
import com.commercepaymentsystem.domain.order.entity.OrderItem;
import com.commercepaymentsystem.domain.order.service.OrderService;
import com.commercepaymentsystem.domain.payment.port.OrderPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentOrderAdapter implements OrderPort {

	private final OrderService orderService;

	@Override
	public ConfirmedOrder confirmOrder(Long orderId, Long memberId) {
		Order order = orderService.getOrderByIdWithOrderItems(orderId);
		orderService.validateOwner(order, memberId);
		orderService.confirmOrder(order);

		List<Long> cartItemIds = order.getOrderItems().stream()
			.map(OrderItem::getSourceCartItemId)
			.filter(id -> id != null)
			.distinct()
			.toList();

		return new ConfirmedOrder(cartItemIds);
	}
}
