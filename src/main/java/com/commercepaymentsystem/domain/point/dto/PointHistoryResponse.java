package com.commercepaymentsystem.domain.point.dto;

import java.time.LocalDateTime;

public record PointHistoryResponse(
	String type,
	Long amount,
	LocalDateTime createdAt
) {
}