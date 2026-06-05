package com.commercepaymentsystem.domain.order.dto;

import java.util.List;

import jakarta.validation.constraints.PositiveOrZero;

public record OrderCreateRequest(

	List<Long> cartItemIds,

	@PositiveOrZero(message = "사용 포인트는 0 이상이어야 합니다.")
	Long usedPointAmount
) {

	public Long safeUsedPointAmount() {
		if (usedPointAmount == null) {
			return 0L;
		}

		return usedPointAmount;
	}
}