package com.commercepaymentsystem.domain.order.dto;

import java.util.List;

public record OrderPreviewRequest(

	List<Long> cartItemIds
) {
}