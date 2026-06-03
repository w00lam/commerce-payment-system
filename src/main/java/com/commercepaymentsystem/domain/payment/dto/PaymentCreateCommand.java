package com.commercepaymentsystem.domain.payment.dto;

import com.commercepaymentsystem.domain.order.entity.Order;

public record PaymentCreateCommand(
	Long memberId,
	Long orderId,
	Long totalOrderAmount,
	Long usedPointAmount,
	Long finalPaymentAmount
) {
	public static PaymentCreateCommand from(
		Order order
	) {
		return new PaymentCreateCommand(
			order.getMemberId(),
			order.getId(),
			order.getTotalPrice(),
			order.getUsedPointAmount(),
			order.getTotalPrice() - order.getUsedPointAmount()
		);
	}
}
