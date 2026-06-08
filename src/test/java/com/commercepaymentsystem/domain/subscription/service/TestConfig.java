package com.commercepaymentsystem.domain.subscription.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.mockito.Mockito;

@Profile("test")
@Configuration
public class TestConfig {

	@Bean
	@Primary
	public SubscriptionPaymentService subscriptionPaymentService() {
		SubscriptionPaymentService mockService = Mockito.mock(SubscriptionPaymentService.class);
		
		// 기본적으로 성공 응답 반환
		when(mockService.pay(anyString(), anyString(), any(), anyString()))
			.thenAnswer(invocation -> {
				String portonePaymentId = invocation.getArgument(0);
				String billingKey = invocation.getArgument(1);
				if ("FAIL_KEY".equals(billingKey)) {
					return SubscriptionPaymentService.PaymentResult.fail(portonePaymentId, "한도 초과");
				}
				return SubscriptionPaymentService.PaymentResult.succeed(portonePaymentId);
			});
			
		return mockService;
	}
}
