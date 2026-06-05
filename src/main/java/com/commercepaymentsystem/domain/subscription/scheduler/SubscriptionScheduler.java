package com.commercepaymentsystem.domain.subscription.scheduler;

import com.commercepaymentsystem.domain.subscription.entity.Subscription;
import com.commercepaymentsystem.domain.subscription.entity.SubscriptionStatus;
import com.commercepaymentsystem.domain.subscription.repository.SubscriptionRepository;
import com.commercepaymentsystem.domain.subscription.service.SubscriptionService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableScheduling
public class SubscriptionScheduler {

	private final SubscriptionRepository subscriptionRepository;
	private final SubscriptionService subscriptionService;
	private final ThreadPoolTaskExecutor executor;

	public SubscriptionScheduler(SubscriptionRepository subscriptionRepository, SubscriptionService subscriptionService) {
		this.subscriptionRepository = subscriptionRepository;
		this.subscriptionService = subscriptionService;

		this.executor = new ThreadPoolTaskExecutor();
		this.executor.setCorePoolSize(10);
		this.executor.setMaxPoolSize(20);
		this.executor.setQueueCapacity(500);
		this.executor.setThreadNamePrefix("sub-billing-");
		this.executor.setWaitForTasksToCompleteOnShutdown(true);
		this.executor.setAwaitTerminationSeconds(30);
		this.executor.initialize();
	}

	/**
	 * 매일 00:00 (KST) 실행
	 * 오늘이 다음 결제일인 활성 구독들을 찾아 정기 결제를 진행합니다.
	 */
	@Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
	public void runSubscriptionBilling() {
		LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
		log.info("Starting regular subscription billing scheduler for date: {}", today);

		int pageSize = 100;
		Long lastId = 0L;
		boolean hasNext = true;
		long totalProcessed = 0;

		while (hasNext) {
			Slice<Subscription> dueSubscriptions = subscriptionRepository
				.findAllByStatusAndNextBillingDateLessThanEqualAndIdGreaterThanOrderByIdAsc(
					SubscriptionStatus.ACTIVE,
					today,
					lastId,
					PageRequest.of(0, pageSize)
				);

			log.info("Fetched {} subscriptions due or overdue for billing in this slice.", dueSubscriptions.getNumberOfElements());

			if (dueSubscriptions.isEmpty()) {
				break;
			}

			List<CompletableFuture<Void>> futures = new ArrayList<>();
			for (Subscription subscription : dueSubscriptions.getContent()) {
				futures.add(CompletableFuture.runAsync(() -> {
					try {
						subscriptionService.processBillingWithLock(subscription.getId(), today);
						log.info("Billing/Status update successfully processed for subscription ID: {}", subscription.getId());
					} catch (Exception e) {
						log.error("Failed to process billing for subscription ID: {}", subscription.getId(), e);
					}
				}, executor));
				lastId = subscription.getId();
				totalProcessed++;
			}

			// 현재 슬라이스/페이지의 결제 통신 작업이 모두 끝날 때까지 대기하여 다음 슬라이스 조회
			CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

			hasNext = dueSubscriptions.hasNext();
		}

		log.info("Subscription billing scheduler completed for date: {}. Total processed: {}", today, totalProcessed);
	}
}
