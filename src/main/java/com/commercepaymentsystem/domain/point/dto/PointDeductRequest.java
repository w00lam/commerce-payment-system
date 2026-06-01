package com.commercepaymentsystem.domain.point.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PointDeductRequest(
	@NotNull(message = "차감 금액은 필수입니다.")
	@Min(value = 1, message = "차감 금액은 0보다 커야 합니다.")
	Long amount,

	@NotNull(message = "결제 식별자는 필수입니다.")
	Long paymentId
) {
}
