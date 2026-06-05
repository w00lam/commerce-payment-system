package com.commercepaymentsystem.domain.subscription.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class StartSubscriptionRequest {

	@NotNull(message = "요금제 ID는 필수입니다.")
	private Long planId;

	@NotNull(message = "결제 수단 ID는 필수입니다.")
	private Long paymentMethodId;
}
