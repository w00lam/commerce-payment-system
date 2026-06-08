package com.commercepaymentsystem.domain.subscription.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.commercepaymentsystem.domain.subscription.dto.StartSubscriptionRequest;
import com.commercepaymentsystem.domain.subscription.entity.PaymentMethod;
import com.commercepaymentsystem.domain.subscription.entity.Plan;
import com.commercepaymentsystem.domain.subscription.entity.Subscription;
import com.commercepaymentsystem.domain.subscription.entity.SubscriptionInvoice;
import com.commercepaymentsystem.domain.subscription.entity.SubscriptionStatus;
import com.commercepaymentsystem.domain.subscription.exception.SubscriptionErrorCode;
import com.commercepaymentsystem.domain.subscription.exception.SubscriptionException;
import com.commercepaymentsystem.domain.subscription.port.MembershipRewardPolicy;
import com.commercepaymentsystem.domain.subscription.port.MembershipRewardPolicyPort;
import com.commercepaymentsystem.domain.subscription.repository.PaymentMethodRepository;
import com.commercepaymentsystem.domain.subscription.repository.PlanRepository;
import com.commercepaymentsystem.domain.subscription.repository.SubscriptionInvoiceRepository;
import com.commercepaymentsystem.domain.subscription.repository.SubscriptionRepository;

@Service
public class SubscriptionBillingPreparationService {

	private static final DateTimeFormatter BILLING_PERIOD_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

	private final PaymentMethodRepository paymentMethodRepository;
	private final PlanRepository planRepository;
	private final SubscriptionRepository subscriptionRepository;
	private final SubscriptionInvoiceRepository subscriptionInvoiceRepository;
	private final MembershipRewardPolicyPort membershipRewardPolicyPort;

	public SubscriptionBillingPreparationService(
		PaymentMethodRepository paymentMethodRepository,
		PlanRepository planRepository,
		SubscriptionRepository subscriptionRepository,
		SubscriptionInvoiceRepository subscriptionInvoiceRepository,
		MembershipRewardPolicyPort membershipRewardPolicyPort
	) {
		this.paymentMethodRepository = paymentMethodRepository;
		this.planRepository = planRepository;
		this.subscriptionRepository = subscriptionRepository;
		this.subscriptionInvoiceRepository = subscriptionInvoiceRepository;
		this.membershipRewardPolicyPort = membershipRewardPolicyPort;
	}

	/**
	 * 구독 생성과 첫 청구서 PENDING 생성을 하나의 독립 트랜잭션에서 준비합니다.
	 *
	 * <p>외부 PG 호출 전에 필요한 DB 상태만 확정하고, 결제 결과 반영은 finalizer가 별도
	 * 트랜잭션에서 처리합니다.</p>
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public PreparedSubscriptionBilling prepareFirstBilling(Long memberId, StartSubscriptionRequest request) {
		Plan plan = planRepository.findById(request.getPlanId())
			.orElseThrow(() -> new SubscriptionException(SubscriptionErrorCode.PLAN_NOT_FOUND));

		PaymentMethod paymentMethod = paymentMethodRepository.findById(request.getPaymentMethodId())
			.orElseThrow(() -> new SubscriptionException(SubscriptionErrorCode.PAYMENT_METHOD_NOT_FOUND));

		if (!paymentMethod.getMemberId().equals(memberId)) {
			throw new SubscriptionException(SubscriptionErrorCode.PAYMENT_METHOD_NOT_FOUND);
		}

		Optional<Subscription> activeSubscription = subscriptionRepository.findByMemberIdAndStatus(
			memberId,
			SubscriptionStatus.ACTIVE
		);
		if (activeSubscription.isPresent()) {
			throw new SubscriptionException(SubscriptionErrorCode.DUPLICATE_SUBSCRIPTION);
		}

		Subscription subscription = subscriptionRepository.save(Subscription.create(memberId, plan, paymentMethod));
		MembershipRewardPolicy rewardPolicy = membershipRewardPolicyPort.getRewardPolicy(memberId);

		String billingPeriod = subscription.getStartedAt().toLocalDate().format(BILLING_PERIOD_FORMATTER);
		String portonePaymentId = "sub-first-" + UUID.randomUUID().toString().substring(0, 8);

		SubscriptionInvoice invoice = subscriptionInvoiceRepository.save(SubscriptionInvoice.createPending(
			subscription,
			billingPeriod,
			portonePaymentId,
			plan.getMonthlyAmount(),
			rewardPolicy.gradeName(),
			rewardPolicy.pointRewardRate()
		));

		return new PreparedSubscriptionBilling(
			subscription.getId(),
			invoice.getId(),
			paymentMethod.getPortoneBillingKey(),
			plan.getMonthlyAmount(),
			plan.getName()
		);
	}

	/**
	 * 스케줄러 결제 대상 구독을 비관적 락으로 잠그고 해당 청구월의 PENDING 인보이스를 생성합니다.
	 *
	 * <p>동시 스케줄 실행이나 재시도 상황에서 같은 구독/청구월 인보이스가 중복 생성되지 않도록
	 * 기존 인보이스 존재 여부를 트랜잭션 안에서 확인합니다.</p>
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public SubscriptionBillingPreparationResult prepareScheduledBilling(Long subscriptionId, LocalDate today) {
		Subscription subscription = subscriptionRepository.findByIdWithPessimisticLock(subscriptionId)
			.orElseThrow(() -> new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));

		if (subscription.getStatus() != SubscriptionStatus.ACTIVE || subscription.getNextBillingDate().isAfter(today)) {
			return SubscriptionBillingPreparationResult.skipped();
		}

		String billingPeriod = subscription.getNextBillingDate().format(BILLING_PERIOD_FORMATTER);
		if (hasInvoiceForBillingPeriod(subscription, billingPeriod, today)) {
			return SubscriptionBillingPreparationResult.skipped();
		}

		Plan plan = subscription.getPlan();
		PaymentMethod paymentMethod = subscription.getPaymentMethod();
		MembershipRewardPolicy rewardPolicy = membershipRewardPolicyPort.getRewardPolicy(subscription.getMemberId());
		String portonePaymentId = "sub-sched-" + subscription.getId() + "-" + today;

		SubscriptionInvoice invoice = subscriptionInvoiceRepository.save(SubscriptionInvoice.createPending(
			subscription,
			billingPeriod,
			portonePaymentId,
			plan.getMonthlyAmount(),
			rewardPolicy.gradeName(),
			rewardPolicy.pointRewardRate()
		));

		return SubscriptionBillingPreparationResult.ready(new PreparedSubscriptionBilling(
			subscription.getId(),
			invoice.getId(),
			paymentMethod.getPortoneBillingKey(),
			plan.getMonthlyAmount(),
			plan.getName()
		));
	}

	private boolean hasInvoiceForBillingPeriod(Subscription subscription, String billingPeriod, LocalDate today) {
		if (subscription.getNextBillingDate().isBefore(today)) {
			return subscriptionInvoiceRepository.findBySubscriptionIdAndBillingPeriod(subscription.getId(), billingPeriod)
				.isPresent();
		}
		return subscriptionInvoiceRepository.existsBySubscriptionIdAndBillingPeriod(subscription.getId(), billingPeriod);
	}
}
