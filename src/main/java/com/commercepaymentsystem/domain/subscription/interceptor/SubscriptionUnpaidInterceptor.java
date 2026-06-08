package com.commercepaymentsystem.domain.subscription.interceptor;

import com.commercepaymentsystem.domain.subscription.entity.SubscriptionStatus;
import com.commercepaymentsystem.domain.subscription.exception.SubscriptionErrorCode;
import com.commercepaymentsystem.domain.subscription.exception.SubscriptionException;
import com.commercepaymentsystem.domain.subscription.repository.SubscriptionRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class SubscriptionUnpaidInterceptor implements HandlerInterceptor {

	private final SubscriptionRepository subscriptionRepository;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		
		if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
			// SecurityConfig에서 memberId를 name으로 저장한다고 가정 (일반적인 JWT 설정)
			Long memberId = Long.parseLong(authentication.getName());

			if (subscriptionRepository.existsByMemberIdAndStatusAndUnpaidTrue(memberId, SubscriptionStatus.ACTIVE)) {
				throw new SubscriptionException(SubscriptionErrorCode.UNPAID_SUBSCRIPTION);
			}
		}

		return true;
	}
}
