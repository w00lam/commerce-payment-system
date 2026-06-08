package com.commercepaymentsystem.domain.subscription.scheduler;

import java.time.LocalDate;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.commercepaymentsystem.domain.subscription.entity.InvoiceStatus;
import com.commercepaymentsystem.domain.subscription.entity.Subscription;
import com.commercepaymentsystem.domain.subscription.entity.SubscriptionInvoice;
import com.commercepaymentsystem.domain.subscription.entity.SubscriptionStatus;
import com.commercepaymentsystem.domain.subscription.repository.SubscriptionInvoiceRepository;
import com.commercepaymentsystem.domain.subscription.repository.SubscriptionRepository;
import com.commercepaymentsystem.domain.subscription.service.SubscriptionPaymentService;
import com.commercepaymentsystem.domain.subscription.service.SubscriptionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionUnpaidRetryScheduler {

	private final SubscriptionRepository subscriptionRepository;
	private final SubscriptionInvoiceRepository subscriptionInvoiceRepository;
	private final SubscriptionService subscriptionService;
	private final SubscriptionPaymentService subscriptionPaymentService;
	private final TransactionTemplate transactionTemplate;

	/**
	 * 매일 오후 14:00 (KST) 미납 요금 재시도 실행
	 */
	@Scheduled(cron = "0 0 14 * * *")
	public void retryUnpaidBilling() {
		log.info("Starting unpaid subscription billing retry...");
		
		// 미납 상태인 모든 활성 구독 조회 (실무에선 Slice/Page 처리가 필요하나 우선 간단히 구현)
		List<Subscription> unpaidSubscriptions = subscriptionRepository.findAll().stream()
			.filter(s -> s.isUnpaid() && s.getStatus() == SubscriptionStatus.ACTIVE)
			.toList();

		for (Subscription subscription : unpaidSubscriptions) {
			List<SubscriptionInvoice> failedInvoices = subscriptionInvoiceRepository.findAllBySubscriptionIdAndStatus(subscription.getId(), InvoiceStatus.FAILED);
			
			for (SubscriptionInvoice invoice : failedInvoices) {
				try {
					log.info("Retrying billing for failed invoice: {}, subscription: {}", invoice.getId(), subscription.getId());
					
					// 실제 결제 요청 (SubscriptionPaymentService 활용)
					var result = subscriptionPaymentService.pay(
						subscription.getPaymentMethod().getPortoneBillingKey(),
						invoice.getBillingAmount(),
						"미납 요금 재청구 - " + invoice.getBillingPeriod()
					);

					if (result.isSuccess()) {
						finalizeRetrySuccess(subscription.getId(), invoice.getId());
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

	private void finalizeRetrySuccess(Long subscriptionId, Long invoiceId) {
		transactionTemplate.executeWithoutResult(status -> {
			Subscription subscription = subscriptionRepository.findById(subscriptionId).orElseThrow();
			SubscriptionInvoice invoice = subscriptionInvoiceRepository.findById(invoiceId).orElseThrow();
			
			invoice.markAsSucceeded(0L, java.time.LocalDateTime.now()); // 재시도 시 포인트 추가 적립은 정책에 따라 결정 (우선 0)
			subscriptionInvoiceRepository.save(invoice);
			
			// 더 이상 실패한 인보이스가 없으면 미납 플래그 해제
			boolean hasRemainingFailed = subscriptionInvoiceRepository.existsBySubscriptionIdAndStatus(subscriptionId, InvoiceStatus.FAILED);
			if (!hasRemainingFailed) {
				subscription.clearUnpaid();
				subscriptionRepository.save(subscription);
			}
		});
	}
}
