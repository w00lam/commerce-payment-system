package com.commercepaymentsystem.domain.refund.adapter;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.commercepaymentsystem.domain.order.entity.Order;
import com.commercepaymentsystem.domain.order.entity.OrderItem;
import com.commercepaymentsystem.domain.order.service.OrderService;
import com.commercepaymentsystem.domain.refund.port.RefundOrderPort;
import com.commercepaymentsystem.domain.refund.port.RefundOrderPort.RefundableOrderInfo;
import com.commercepaymentsystem.domain.refund.port.RefundOrderPort.RefundableOrderInfo.RefundableOrderItemInfo;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RefundOrderAdapter implements RefundOrderPort {

	private final OrderService orderService;

	@Override
	public RefundableOrderInfo getRefundableOrder(Long orderId, Long memberId) {
		Order order = orderService.getOrderByIdWithOrderItems(orderId);
		orderService.validateOwner(order, memberId);
		return toRefundableOrderInfo(order);
	}

	@Override
	public Map<Long, Long> restoreProductStock(Long orderId, Map<Long, Long> refundQuantities) {
		Order order = orderService.getOrderById(orderId);
		return orderService.restoreProductStock(order, refundQuantities);
	}

	@Override
	public void cancelOrder(Long orderId) {
		Order order = orderService.getOrderById(orderId);
		orderService.cancelOrder(order);
	}

	private RefundableOrderInfo toRefundableOrderInfo(Order order) {
		return new RefundableOrderInfo(
			order.getId(),
			order.getMemberId(),
			order.getOrderItems().stream()
				.map(this::toRefundableOrderItemInfo)
				.toList()
		);
	}

	private RefundableOrderItemInfo toRefundableOrderItemInfo(OrderItem orderItem) {
		return new RefundableOrderItemInfo(
			orderItem.getId(),
			orderItem.getQuantity(),
			orderItem.getOrderPrice()
		);
	}
}
