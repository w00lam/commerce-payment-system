package com.commercepaymentsystem.domain.subscription.dto;

import com.commercepaymentsystem.domain.subscription.entity.Subscription;
import com.commercepaymentsystem.domain.subscription.entity.SubscriptionStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SubscriptionResponse {
	private Long id;
	private String planName;
	private Long monthlyAmount;
	private SubscriptionStatus status;
	private LocalDate nextBillingDate;
	private LocalDateTime startedAt;

	public static SubscriptionResponse from(Subscription subscription) {
		return new SubscriptionResponse(
			subscription.getId(),
			subscription.getPlan().getName(),
			subscription.getPlan().getMonthlyAmount(),
			subscription.getStatus(),
			subscription.getNextBillingDate(),
			subscription.getStartedAt()
		);
	}
}
