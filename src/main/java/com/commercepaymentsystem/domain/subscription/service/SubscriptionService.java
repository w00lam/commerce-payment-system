package com.commercepaymentsystem.domain.subscription.service;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commercepaymentsystem.domain.subscription.dto.RegisterPaymentMethodRequest;
import com.commercepaymentsystem.domain.subscription.dto.StartSubscriptionRequest;
import com.commercepaymentsystem.domain.subscription.dto.SubscriptionResponse;
import com.commercepaymentsystem.domain.subscription.entity.PaymentMethod;
import com.commercepaymentsystem.domain.subscription.entity.Subscription;
import com.commercepaymentsystem.domain.subscription.entity.SubscriptionStatus;
import com.commercepaymentsystem.domain.subscription.exception.SubscriptionErrorCode;
import com.commercepaymentsystem.domain.subscription.exception.SubscriptionException;
import com.commercepaymentsystem.domain.subscription.repository.PaymentMethodRepository;
import com.commercepaymentsystem.domain.subscription.repository.SubscriptionRepository;

@Service
public class SubscriptionService {

	private final PaymentMethodRepository paymentMethodRepository;
	private final SubscriptionRepository subscriptionRepository;
	private final SubscriptionBillingPreparationService billingPreparationService;
	private final SubscriptionPaymentOrchestrator paymentOrchestrator;
	private final SubscriptionBillingFinalizer billingFinalizer;
	private final SubscriptionQueryService subscriptionQueryService;

	public SubscriptionService(
		PaymentMethodRepository paymentMethodRepository,
		SubscriptionRepository subscriptionRepository,
		SubscriptionBillingPreparationService billingPreparationService,
		SubscriptionPaymentOrchestrator paymentOrchestrator,
		SubscriptionBillingFinalizer billingFinalizer,
		SubscriptionQueryService subscriptionQueryService
	) {
		this.paymentMethodRepository = paymentMethodRepository;
		this.subscriptionRepository = subscriptionRepository;
		this.billingPreparationService = billingPreparationService;
		this.paymentOrchestrator = paymentOrchestrator;
		this.billingFinalizer = billingFinalizer;
		this.subscriptionQueryService = subscriptionQueryService;
	}

	@Transactional
	public PaymentMethod registerPaymentMethod(Long memberId, RegisterPaymentMethodRequest request) {
		Optional<PaymentMethod> existing = paymentMethodRepository.findByPortoneBillingKey(request.getPortoneBillingKey());
		if (existing.isPresent()) {
			if (!existing.get().getMemberId().equals(memberId)) {
				throw new SubscriptionException(SubscriptionErrorCode.DUPLICATE_BILLING_KEY);
			}
			return existing.get();
		}

		PaymentMethod paymentMethod = new PaymentMethod(
			memberId,
			request.getPortoneBillingKey(),
			request.getCardCompanyName()
		);
		return paymentMethodRepository.save(paymentMethod);
	}

	/**
	 * 구독 시작 흐름을 준비 트랜잭션, 외부 PG 호출, 결과 반영 트랜잭션 순서로 조율합니다.
	 */
	public SubscriptionResponse startSubscription(Long memberId, StartSubscriptionRequest request) {
		PreparedSubscriptionBilling billing = billingPreparationService.prepareFirstBilling(memberId, request);
		SubscriptionPaymentService.PaymentResult paymentResult = paymentOrchestrator.pay(
			billing,
			"첫 결제 PG API 호출 중 예외 발생"
		);

		billingFinalizer.finalizeFirstBilling(memberId, billing, paymentResult);

		if (!paymentResult.isSuccess()) {
			throw new SubscriptionException(
				SubscriptionErrorCode.FIRST_PAYMENT_FAILED,
				"첫 결제 실패: " + paymentResult.getFailureReason()
			);
		}

		return subscriptionQueryService.getSubscriptionResponse(billing.subscriptionId());
	}

	@Transactional
	public void cancelSubscription(Long memberId, Long subscriptionId) {
		Subscription subscription = subscriptionRepository.findById(subscriptionId)
			.orElseThrow(() -> new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));

		if (!subscription.getMemberId().equals(memberId)) {
			throw new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND);
		}

		subscription.cancel();
		subscriptionRepository.save(subscription);
	}

	@Transactional(readOnly = true)
	public SubscriptionResponse getMySubscription(Long memberId) {
		Subscription subscription = subscriptionRepository.findByMemberIdAndStatus(memberId, SubscriptionStatus.ACTIVE)
			.orElseThrow(() -> new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));

		return SubscriptionResponse.from(subscription);
	}

	/**
	 * 스케줄러가 지정한 구독의 정기 결제 흐름을 준비, PG 호출, 결과 반영 순서로 조율합니다.
	 */
	public void processBillingWithLock(Long subscriptionId, LocalDate today) {
		SubscriptionBillingPreparationResult result = billingPreparationService.prepareScheduledBilling(subscriptionId, today);
		if (result == null || !result.isReady()) {
			return;
		}

		SubscriptionPaymentService.PaymentResult paymentResult = paymentOrchestrator.pay(
			result.billing(),
			"PG 결제 API 호출 중 예외 발생"
		);
		billingFinalizer.finalizeScheduledBilling(result.billing(), paymentResult);
	}
}
