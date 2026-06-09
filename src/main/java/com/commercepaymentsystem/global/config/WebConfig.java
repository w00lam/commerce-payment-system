package com.commercepaymentsystem.global.config;

import com.commercepaymentsystem.domain.subscription.interceptor.SubscriptionUnpaidInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

	private final SubscriptionUnpaidInterceptor subscriptionUnpaidInterceptor;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(subscriptionUnpaidInterceptor)
			.addPathPatterns("/api/v1/orders/**", "/api/v1/payments/**") // 미납 시 제한할 경로 설정
			.excludePathPatterns("/api/v1/subscriptions/**"); // 구독 해지나 미납 해결을 위한 경로는 제외
	}
}
