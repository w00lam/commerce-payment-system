package com.commercepaymentsystem.domain.payment.dto;

import com.commercepaymentsystem.domain.order.entity.Order;

public record PaymentCreateCommand(
	Long memberId,
	Long orderId,
	String orderName,
	Long totalOrderAmount,
	Long usedPointAmount,
	Long finalPaymentAmount
) {
	public PaymentCreateCommand(
		Long memberId,
		Long orderId,
		Long totalOrderAmount,
		Long usedPointAmount,
		Long finalPaymentAmount
	) {
		this(
			memberId,
			orderId,
			"order-" + orderId,
			totalOrderAmount,
			usedPointAmount,
			finalPaymentAmount
		);
	}

	public static PaymentCreateCommand from(
		Order order
	) {
		return new PaymentCreateCommand(
			order.getMemberId(),
			order.getId(),
			order.getOrderName(),
			order.getTotalPrice(),
			order.getUsedPointAmount(),
			order.getTotalPrice() - order.getUsedPointAmount()
		);
	}
}
