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
import org.springframework.beans.factory.annotation.Value;
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
	private final int pageSize;

	public SubscriptionScheduler(
		SubscriptionRepository subscriptionRepository,
		SubscriptionService subscriptionService,
		@org.springframework.beans.factory.annotation.Qualifier("subscriptionBillingExecutor") ThreadPoolTaskExecutor executor,
		@Value("${subscription.billing.scheduler.page-size:100}") int pageSize
	) {
		this.subscriptionRepository = subscriptionRepository;
		this.subscriptionService = subscriptionService;
		this.executor = executor;
		this.pageSize = pageSize;
	}

	/**
	 * 매일 00:00 (KST) 실행
	 * 오늘이 다음 결제일인 활성 구독들을 찾아 정기 결제를 진행합니다.
	 */
	@Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
	public void runSubscriptionBilling() {
		LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
		log.info("Starting regular subscription billing scheduler for date: {}", today);

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
						log.info("Processing billing for subscription ID: {}, member ID: {}", subscription.getId(), subscription.getMemberId());
						subscriptionService.processBillingWithLock(subscription.getId(), today);
						log.info("Successfully processed billing for subscription ID: {}", subscription.getId());
					} catch (Exception e) {
						log.error("CRITICAL ERROR: Failed to process billing for subscription ID: {}. Reason: {}", 
							subscription.getId(), e.getMessage(), e);
						// 개별 실패는 로그로 기록하고 다음 건으로 넘어감
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
