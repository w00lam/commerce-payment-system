package com.commercepaymentsystem.domain.order.dto;

import java.util.List;

public record OrderPreviewResponse(

	Long memberId,
	Long totalAmount,
	List<OrderPreviewItemResponse> items
) {
}