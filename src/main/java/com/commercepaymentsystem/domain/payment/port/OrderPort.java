package com.commercepaymentsystem.domain.payment.port;

import java.util.List;

public interface OrderPort {

	ConfirmedOrder confirmOrder(Long orderId, Long memberId);

	record ConfirmedOrder(List<Long> cartItemIds) {
		public ConfirmedOrder {
			cartItemIds = List.copyOf(cartItemIds);
		}
	}
}
