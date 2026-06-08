package com.commercepaymentsystem.domain.subscription.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class RegisterPaymentMethodRequest {

	@NotBlank(message = "빌링키는 필수입니다.")
	private String portoneBillingKey;

	@NotBlank(message = "카드사 정보는 필수입니다.")
	private String cardCompanyName;
}
