package com.commercepaymentsystem.domain.order.dto;

import java.util.List;

import com.commercepaymentsystem.domain.order.entity.OrderStatus;
import com.commercepaymentsystem.domain.payment.entity.PaymentStatus;

public record OrderCreateResponse(

	Long orderId,
	String orderNumber,
	Long memberId,
	Long totalAmount,
	Long usedPointAmount,
	Long finalPaymentAmount,
	OrderStatus orderStatus,
	String paymentId,
	PaymentStatus paymentStatus,
	List<OrderCreateItemResponse> items
) {
}