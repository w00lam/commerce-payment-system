package com.commercepaymentsystem.domain.subscription.scheduler;

import com.commercepaymentsystem.domain.subscription.entity.Subscription;
import com.commercepaymentsystem.domain.subscription.entity.SubscriptionStatus;
import com.commercepaymentsystem.domain.subscription.repository.SubscriptionRepository;
import com.commercepaymentsystem.domain.subscription.service.SubscriptionService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class SubscriptionScheduler {

	private final SubscriptionRepository subscriptionRepository;
	private final SubscriptionService subscriptionService;

	/**
	 * 매일 00:00 (KST) 실행
	 * 오늘이 다음 결제일인 활성 구독들을 찾아 정기 결제를 진행합니다.
	 */
	@Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
	public void runSubscriptionBilling() {
		LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
		log.info("Starting regular subscription billing scheduler for date: {}", today);

		List<Subscription> dueSubscriptions = subscriptionRepository.findAllByStatusAndNextBillingDateLessThanEqual(
			SubscriptionStatus.ACTIVE,
			today
		);

		log.info("Found {} subscriptions due or overdue for billing.", dueSubscriptions.size());

		for (Subscription subscription : dueSubscriptions) {
			try {
				subscriptionService.processBillingWithLock(subscription.getId(), today);
				log.info("Billing/Status update successfully processed for subscription ID: {}", subscription.getId());
			} catch (Exception e) {
				log.error("Failed to process billing for subscription ID: {}", subscription.getId(), e);
			}
		}

		log.info("Subscription billing scheduler completed for date: {}", today);
	}
}
