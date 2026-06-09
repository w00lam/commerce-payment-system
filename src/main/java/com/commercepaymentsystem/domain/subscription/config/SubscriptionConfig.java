package com.commercepaymentsystem.domain.subscription.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class SubscriptionConfig {

	@Bean(name = "subscriptionBillingExecutor")
	public ThreadPoolTaskExecutor subscriptionBillingExecutor(
		@Value("${subscription.billing.executor.core-pool-size:10}") int corePoolSize,
		@Value("${subscription.billing.executor.max-pool-size:20}") int maxPoolSize,
		@Value("${subscription.billing.executor.queue-capacity:500}") int queueCapacity,
		@Value("${subscription.billing.executor.await-termination-seconds:30}") int awaitTerminationSeconds
	) {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(corePoolSize);
		executor.setMaxPoolSize(maxPoolSize);
		executor.setQueueCapacity(queueCapacity);
		executor.setThreadNamePrefix("sub-billing-");
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds(awaitTerminationSeconds);
		executor.initialize();
		return executor;
	}
}
