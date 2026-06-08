package com.commercepaymentsystem.domain.subscription.scheduler;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.commercepaymentsystem.domain.subscription.entity.InvoiceStatus;
import com.commercepaymentsystem.domain.subscription.entity.Subscription;
import com.commercepaymentsystem.domain.subscription.entity.SubscriptionInvoice;
import com.commercepaymentsystem.domain.subscription.entity.SubscriptionStatus;
import com.commercepaymentsystem.domain.subscription.repository.SubscriptionInvoiceRepository;
import com.commercepaymentsystem.domain.subscription.repository.SubscriptionRepository;
import com.commercepaymentsystem.domain.subscription.service.PreparedSubscriptionBilling;
import com.commercepaymentsystem.domain.subscription.service.SubscriptionBillingFinalizer;
import com.commercepaymentsystem.domain.subscription.service.SubscriptionPaymentOrchestrator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionUnpaidRetryScheduler {

	private final SubscriptionRepository subscriptionRepository;
	private final SubscriptionInvoiceRepository subscriptionInvoiceRepository;
	private final SubscriptionPaymentOrchestrator paymentOrchestrator;
	private final SubscriptionBillingFinalizer billingFinalizer;

	/**
	 * 매일 오후 14:00 (KST) 미납 요금 재시도 실행
	 */
	@Scheduled(cron = "0 0 14 * * *")
	public void retryUnpaidBilling() {
		log.info("Starting unpaid subscription billing retry...");
		
		// 미납 상태인 모든 활성 구독과 결제 수단(PaymentMethod)을 JOIN FETCH로 한 번에 조회 (N+1 및 LazyInitializationException 방지)
		List<Subscription> unpaidSubscriptions = subscriptionRepository.findAllByStatusAndUnpaidTrueWithPaymentMethod(SubscriptionStatus.ACTIVE);

		for (Subscription subscription : unpaidSubscriptions) {
			List<SubscriptionInvoice> failedInvoices = subscriptionInvoiceRepository.findAllBySubscriptionIdAndStatus(subscription.getId(), InvoiceStatus.FAILED);
			
			for (SubscriptionInvoice invoice : failedInvoices) {
				try {
					log.info("Retrying billing for failed invoice: {}, subscription: {}", invoice.getId(), subscription.getId());
					
					String retryPaymentId = "sub-retry-" + invoice.getId() + "-" + java.util.UUID.randomUUID().toString().substring(0, 8);
					PreparedSubscriptionBilling billing = new PreparedSubscriptionBilling(
						subscription.getId(),
						invoice.getId(),
						subscription.getPaymentMethod().getPortoneBillingKey(),
						invoice.getBillingAmount(),
						"미납 요금 재청구 - " + invoice.getBillingPeriod(),
						retryPaymentId
					);

					var result = paymentOrchestrator.pay(billing, "미납 요금 PG API 호출 중 예외 발생");
					billingFinalizer.finalizeRetryBilling(billing, result);
					if (result.isSuccess()) {
						log.info("Successfully recovered unpaid billing for invoice: {}", invoice.getId());
					} else {
						log.warn("Failed to recover unpaid billing for invoice: {}. Reason: {}", invoice.getId(), result.getFailureReason());
					}
				} catch (Exception e) {
					log.error("Error during unpaid billing retry for invoice: {}", invoice.getId(), e);
				}
			}
		}
		log.info("Unpaid subscription billing retry completed.");
	}
}
