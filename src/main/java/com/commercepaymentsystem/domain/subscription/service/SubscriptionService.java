package com.commercepaymentsystem.domain.subscription.service;

import com.commercepaymentsystem.domain.membership.entity.MemberMembership;
import com.commercepaymentsystem.domain.membership.repository.MemberMembershipRepository;
import com.commercepaymentsystem.domain.subscription.dto.RegisterPaymentMethodRequest;
import com.commercepaymentsystem.domain.subscription.dto.StartSubscriptionRequest;
import com.commercepaymentsystem.domain.subscription.dto.SubscriptionResponse;
import com.commercepaymentsystem.domain.subscription.entity.InvoiceStatus;
import com.commercepaymentsystem.domain.subscription.entity.PaymentMethod;
import com.commercepaymentsystem.domain.subscription.entity.Plan;
import com.commercepaymentsystem.domain.subscription.entity.Subscription;
import com.commercepaymentsystem.domain.subscription.entity.SubscriptionInvoice;
import com.commercepaymentsystem.domain.subscription.entity.SubscriptionStatus;
import com.commercepaymentsystem.domain.subscription.exception.SubscriptionErrorCode;
import com.commercepaymentsystem.domain.subscription.exception.SubscriptionException;
import com.commercepaymentsystem.domain.subscription.repository.PaymentMethodRepository;
import com.commercepaymentsystem.domain.subscription.repository.PlanRepository;
import com.commercepaymentsystem.domain.subscription.repository.SubscriptionInvoiceRepository;
import com.commercepaymentsystem.domain.subscription.repository.SubscriptionRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionService {

	private final PaymentMethodRepository paymentMethodRepository;
	private final PlanRepository planRepository;
	private final SubscriptionRepository subscriptionRepository;
	private final SubscriptionInvoiceRepository subscriptionInvoiceRepository;
	private final MemberMembershipRepository memberMembershipRepository;
	private final SubscriptionPaymentService subscriptionPaymentService;

	/**
	 * 결제 수단(빌링키) 등록
	 */
	@Transactional
	public PaymentMethod registerPaymentMethod(Long memberId, RegisterPaymentMethodRequest request) {
		// 중복된 빌링키가 이미 등록되어 있는지 확인
		Optional<PaymentMethod> existing = paymentMethodRepository.findByPortoneBillingKey(request.getPortoneBillingKey());
		if (existing.isPresent()) {
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
	 * 구독 시작 (빌링키 발급 + 첫 결제 즉시 진행)
	 */
	@Transactional
	public SubscriptionResponse startSubscription(Long memberId, StartSubscriptionRequest request) {
		Plan plan = planRepository.findById(request.getPlanId())
			.orElseThrow(() -> new SubscriptionException(SubscriptionErrorCode.PLAN_NOT_FOUND));

		PaymentMethod paymentMethod = paymentMethodRepository.findById(request.getPaymentMethodId())
			.orElseThrow(() -> new SubscriptionException(SubscriptionErrorCode.PAYMENT_METHOD_NOT_FOUND));

		// 본인 소유의 결제 수단인지 검증
		if (!paymentMethod.getMemberId().equals(memberId)) {
			throw new SubscriptionException(SubscriptionErrorCode.PAYMENT_METHOD_NOT_FOUND);
		}

		// 이미 활성화된 구독이 있는지 확인 (중복 구독 방지)
		Optional<Subscription> activeSubscription = subscriptionRepository.findByMemberIdAndStatus(memberId, SubscriptionStatus.ACTIVE);
		if (activeSubscription.isPresent()) {
			throw new SubscriptionException(SubscriptionErrorCode.DUPLICATE_SUBSCRIPTION);
		}

		// 구독 생성
		Subscription subscription = Subscription.create(memberId, plan, paymentMethod);
		subscriptionRepository.save(subscription);

		// 청구 및 결제 처리 준비 (스냅샷 정보 획득)
		String gradeName = "NORMAL";
		int rewardRate = 1;

		Optional<MemberMembership> memberMembership = memberMembershipRepository.findByMemberId(memberId);
		if (memberMembership.isPresent()) {
			gradeName = memberMembership.get().getMembershipGrade().getName();
			rewardRate = memberMembership.get().getMembershipGrade().getPointRewardRate();
		}

		String billingPeriod = subscription.getStartedAt().format(DateTimeFormatter.ofPattern("yyyy-MM"));
		String portonePaymentId = "sub-first-" + UUID.randomUUID().toString().substring(0, 8);

		SubscriptionInvoice invoice = SubscriptionInvoice.createPending(
			subscription,
			billingPeriod,
			portonePaymentId,
			plan.getMonthlyAmount(),
			gradeName,
			rewardRate
		);
		subscriptionInvoiceRepository.save(invoice);

		// 첫 결제 즉시 요청 (구독PaymentService를 통해 가상 승인 처리)
		SubscriptionPaymentService.PaymentResult paymentResult = subscriptionPaymentService.pay(
			paymentMethod.getPortoneBillingKey(),
			plan.getMonthlyAmount(),
			plan.getName()
		);

		if (paymentResult.isSuccess()) {
			// 첫 결제 성공 시 인보이스 상태 업데이트 및 구독 활성화 유지
			long earnedPoints = plan.getMonthlyAmount() * rewardRate / 100;
			invoice.markAsSucceeded(earnedPoints, LocalDateTime.now());
			subscriptionInvoiceRepository.save(invoice);
		} else {
			// 첫 결제 실패 시 구독 즉시 해지(CANCELLED)로 롤백하고 400 예외 반환
			subscription.cancel();
			subscriptionRepository.save(subscription);

			invoice.markAsFailed(paymentResult.getFailureReason());
			subscriptionInvoiceRepository.save(invoice);

			throw new SubscriptionException(
				SubscriptionErrorCode.FIRST_PAYMENT_FAILED,
				"첫 결제 실패: " + paymentResult.getFailureReason()
			);
		}

		return SubscriptionResponse.from(subscription);
	}

	/**
	 * 구독 해지
	 */
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

	/**
	 * 내 구독 조회
	 */
	public SubscriptionResponse getMySubscription(Long memberId) {
		Subscription subscription = subscriptionRepository.findByMemberIdAndStatus(memberId, SubscriptionStatus.ACTIVE)
			.orElseThrow(() -> new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));

		return SubscriptionResponse.from(subscription);
	}

	/**
	 * 스케줄러가 매일 각 활성 구독 청구 처리를 위해 호출하는 비즈니스 메서드
	 */
	@Transactional
	public void executeBilling(Subscription subscription, LocalDate today) {
		Plan plan = subscription.getPlan();
		PaymentMethod paymentMethod = subscription.getPaymentMethod();

		String billingPeriod = today.format(DateTimeFormatter.ofPattern("yyyy-MM"));
		String portonePaymentId = "sub-sched-" + subscription.getId() + "-" + today.toString();

		// 스냅샷 정보 획득
		String gradeName = "NORMAL";
		int rewardRate = 1;

		Optional<MemberMembership> memberMembership = memberMembershipRepository.findByMemberId(subscription.getMemberId());
		if (memberMembership.isPresent()) {
			gradeName = memberMembership.get().getMembershipGrade().getName();
			rewardRate = memberMembership.get().getMembershipGrade().getPointRewardRate();
		}

		SubscriptionInvoice invoice = SubscriptionInvoice.createPending(
			subscription,
			billingPeriod,
			portonePaymentId,
			plan.getMonthlyAmount(),
			gradeName,
			rewardRate
		);
		subscriptionInvoiceRepository.save(invoice);

		// 포트원 정기 결제 API 호출 (가상 스텁 호출)
		SubscriptionPaymentService.PaymentResult paymentResult = subscriptionPaymentService.pay(
			paymentMethod.getPortoneBillingKey(),
			plan.getMonthlyAmount(),
			plan.getName()
		);

		if (paymentResult.isSuccess()) {
			// 결제 성공 시
			long earnedPoints = plan.getMonthlyAmount() * rewardRate / 100;
			invoice.markAsSucceeded(earnedPoints, LocalDateTime.now());
			subscriptionInvoiceRepository.save(invoice);

			// 다음 결제일 갱신 (말일 클램프 로직 반영)
			subscription.renewNextBillingDate();
			subscriptionRepository.save(subscription);
		} else {
			// 결제 실패 시 청구서만 실패로 기록하고 구독은 활성(ACTIVE) 상태 유지
			invoice.markAsFailed(paymentResult.getFailureReason());
			subscriptionInvoiceRepository.save(invoice);
		}
	}
}
