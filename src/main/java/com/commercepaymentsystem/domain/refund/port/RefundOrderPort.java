package com.commercepaymentsystem.domain.refund.port;

import java.util.List;
import java.util.Map;

public interface RefundOrderPort {

	RefundableOrderInfo getRefundableOrder(Long orderId, Long memberId);

	Map<Long, Long> restoreProductStock(Long orderId, Map<Long, Long> refundQuantities);

	void cancelOrder(Long orderId);

	record RefundableOrderInfo(
		Long orderId,
		Long memberId,
		List<RefundableOrderItemInfo> orderItems
	) {
		public RefundableOrderInfo {
			orderItems = List.copyOf(orderItems);
		}

		public record RefundableOrderItemInfo(
			Long orderItemId,
			Long quantity,
			Long orderPrice
		) {
		}
	}
}
