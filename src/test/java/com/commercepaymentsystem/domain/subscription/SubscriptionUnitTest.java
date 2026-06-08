package com.commercepaymentsystem.domain.subscription;

import com.commercepaymentsystem.domain.subscription.entity.Subscription;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionUnitTest {

	@Test
	void renewNextBillingDate_Jan31ToFeb28_ClampsToFeb28() {
		// Given: 1월 31일에 가입하여 현재 다음 결제일이 1월 31일인 상황
		LocalDateTime startedAt = LocalDateTime.of(2026, 1, 31, 0, 0);
		LocalDate currentNextBillingDate = LocalDate.of(2026, 1, 31);
		Subscription subscription = new Subscription(startedAt, currentNextBillingDate);

		// When: 다음 결제일 갱신 실행
		subscription.renewNextBillingDate();

		// Then: 다음 결제일은 2월 28일이 됨 (2월 말일로 클램핑)
		assertThat(subscription.getNextBillingDate()).isEqualTo(LocalDate.of(2026, 2, 28));
	}

	@Test
	void renewNextBillingDate_Feb28ToMar31_RestoresToMar31() {
		// Given: 1월 31일에 가입했고 현재 다음 결제일이 2월 28일인 상황
		LocalDateTime startedAt = LocalDateTime.of(2026, 1, 31, 0, 0);
		LocalDate currentNextBillingDate = LocalDate.of(2026, 2, 28);
		Subscription subscription = new Subscription(startedAt, currentNextBillingDate);

		// When: 다음 결제일 갱신 실행
		subscription.renewNextBillingDate();

		// Then: 다음 결제일은 3월 31일이 됨 (기존 가입일자인 31일로 복원)
		assertThat(subscription.getNextBillingDate()).isEqualTo(LocalDate.of(2026, 3, 31));
	}

	@Test
	void renewNextBillingDate_Jan30ToFeb28_ClampsToFeb28() {
		// Given: 1월 30일에 가입하여 현재 다음 결제일이 1월 30일인 상황
		LocalDateTime startedAt = LocalDateTime.of(2026, 1, 30, 0, 0);
		LocalDate currentNextBillingDate = LocalDate.of(2026, 1, 30);
		Subscription subscription = new Subscription(startedAt, currentNextBillingDate);

		// When: 다음 결제일 갱신 실행
		subscription.renewNextBillingDate();

		// Then: 다음 결제일은 2월 28일이 됨 (2월 말일로 클램핑)
		assertThat(subscription.getNextBillingDate()).isEqualTo(LocalDate.of(2026, 2, 28));
	}

	@Test
	void renewNextBillingDate_Feb28ToMar30_RestoresToMar30() {
		// Given: 1월 30일에 가입했고 현재 다음 결제일이 2월 28일(클램핑된 상태)인 상황
		LocalDateTime startedAt = LocalDateTime.of(2026, 1, 30, 0, 0);
		LocalDate currentNextBillingDate = LocalDate.of(2026, 2, 28);
		Subscription subscription = new Subscription(startedAt, currentNextBillingDate);

		// When: 다음 결제일 갱신 실행
		subscription.renewNextBillingDate();

		// Then: 다음 결제일은 3월 30일이 됨 (기존 가입일자인 30일로 복원)
		assertThat(subscription.getNextBillingDate()).isEqualTo(LocalDate.of(2026, 3, 30));
	}
}
