package com.commercepaymentsystem.domain.membership.event;

import com.commercepaymentsystem.domain.membership.service.MembershipService;
import com.commercepaymentsystem.domain.subscription.event.SubscriptionPaymentSucceededEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubscriptionPaymentSucceededEventListener {

	private final MembershipService membershipService;

	@EventListener
	public void handle(SubscriptionPaymentSucceededEvent event) {
		membershipService.applyPayment(event.memberId(), event.paidAmount());
	}
}
