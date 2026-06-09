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
			// JwtAuthFilter에서 principal 객체 자체에 Long 타입의 memberId를 세팅하므로 직접 캐스팅
			Long memberId = (Long) authentication.getPrincipal();

			if (subscriptionRepository.existsByMemberIdAndStatusAndUnpaidTrue(memberId, SubscriptionStatus.ACTIVE)) {
				throw new SubscriptionException(SubscriptionErrorCode.UNPAID_SUBSCRIPTION);
			}
		}

		return true;
	}
}
