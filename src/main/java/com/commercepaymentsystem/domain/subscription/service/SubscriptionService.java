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
import com.commercepaymentsystem.domain.point.service.PointService;
import com.commercepaymentsystem.domain.point.entity.PointSourceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SubscriptionService {

	private final PaymentMethodRepository paymentMethodRepository;
	private final PlanRepository planRepository;
	private final SubscriptionRepository subscriptionRepository;
	private final SubscriptionInvoiceRepository subscriptionInvoiceRepository;
	private final MemberMembershipRepository memberMembershipRepository;
	private final SubscriptionPaymentService subscriptionPaymentService;
	private final PointService pointService;
	private final PlatformTransactionManager transactionManager;
	private final TransactionTemplate requiresNewTxTemplate;

	public SubscriptionService(
		PaymentMethodRepository paymentMethodRepository,
		PlanRepository planRepository,
		SubscriptionRepository subscriptionRepository,
		SubscriptionInvoiceRepository subscriptionInvoiceRepository,
		MemberMembershipRepository memberMembershipRepository,
		SubscriptionPaymentService subscriptionPaymentService,
		PointService pointService,
		PlatformTransactionManager transactionManager
	) {
		this.paymentMethodRepository = paymentMethodRepository;
		this.planRepository = planRepository;
		this.subscriptionRepository = subscriptionRepository;
		this.subscriptionInvoiceRepository = subscriptionInvoiceRepository;
		this.memberMembershipRepository = memberMembershipRepository;
		this.subscriptionPaymentService = subscriptionPaymentService;
		this.pointService = pointService;
		this.transactionManager = transactionManager;

		this.requiresNewTxTemplate = new TransactionTemplate(transactionManager);
		this.requiresNewTxTemplate.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	}

	private static class StartSubscriptionTxResult {
		private final Long subscriptionId;
		private final Long invoiceId;
		private final String billingKey;
		private final Long billingAmount;
		private final String planName;

		public StartSubscriptionTxResult(Long subscriptionId, Long invoiceId, String billingKey, Long billingAmount, String planName) {
			this.subscriptionId = subscriptionId;
			this.invoiceId = invoiceId;
			this.billingKey = billingKey;
			this.billingAmount = billingAmount;
			this.planName = planName;
		}

		public Long getSubscriptionId() { return subscriptionId; }
		public Long getInvoiceId() { return invoiceId; }
		public String getBillingKey() { return billingKey; }
		public Long getBillingAmount() { return billingAmount; }
		public String getPlanName() { return planName; }
	}

	public static class PrepareBillingResult {
		public enum Status { READY, SKIPPED, CANCELLED }
		private final Status status;
		private final Long subscriptionId;
		private final Long invoiceId;
		private final String billingKey;
		private final Long billingAmount;
		private final String planName;

		private PrepareBillingResult(Status status, Long subscriptionId, Long invoiceId, String billingKey, Long billingAmount, String planName) {
			this.status = status;
			this.subscriptionId = subscriptionId;
			this.invoiceId = invoiceId;
			this.billingKey = billingKey;
			this.billingAmount = billingAmount;
			this.planName = planName;
		}

		public static PrepareBillingResult ready(Long subscriptionId, Long invoiceId, String billingKey, Long billingAmount, String planName) {
			return new PrepareBillingResult(Status.READY, subscriptionId, invoiceId, billingKey, billingAmount, planName);
		}

		public static PrepareBillingResult skipped() {
			return new PrepareBillingResult(Status.SKIPPED, null, null, null, null, null);
		}

		public static PrepareBillingResult cancelled() {
			return new PrepareBillingResult(Status.CANCELLED, null, null, null, null, null);
		}

		public Status getStatus() { return status; }
		public Long getSubscriptionId() { return subscriptionId; }
		public Long getInvoiceId() { return invoiceId; }
		public String getBillingKey() { return billingKey; }
		public Long getBillingAmount() { return billingAmount; }
		public String getPlanName() { return planName; }
	}

	/**
	 * 결제 수단(빌링키) 등록
	 */
	@Transactional
	public PaymentMethod registerPaymentMethod(Long memberId, RegisterPaymentMethodRequest request) {
		// 중복된 빌링키가 이미 등록되어 있는지 확인
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
	 * 구독 시작 (빌링키 발급 + 첫 결제 즉시 진행)
	 */
	public SubscriptionResponse startSubscription(Long memberId, StartSubscriptionRequest request) {
		// 1. Transaction 1: Create subscription and pending invoice
		StartSubscriptionTxResult txResult = requiresNewTxTemplate.execute(status -> {
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

			// 결제 주기를 가입 예정일 기준으로 통일
			String billingPeriod = subscription.getStartedAt().toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM"));
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

			return new StartSubscriptionTxResult(
				subscription.getId(),
				invoice.getId(),
				paymentMethod.getPortoneBillingKey(),
				plan.getMonthlyAmount(),
				plan.getName()
			);
		});

		// 2. Call external PG API outside transaction with try-catch safety net
		SubscriptionPaymentService.PaymentResult paymentResult;
		try {
			paymentResult = subscriptionPaymentService.pay(
				txResult.getBillingKey(),
				txResult.getBillingAmount(),
				txResult.getPlanName()
			);
		} catch (Exception e) {
			paymentResult = SubscriptionPaymentService.PaymentResult.fail("첫 결제 PG API 호출 중 예외 발생: " + e.getMessage());
		}

		final SubscriptionPaymentService.PaymentResult finalPaymentResult = paymentResult;

		// 3. Transaction 2: Complete subscription status (Success/Failure)
		requiresNewTxTemplate.executeWithoutResult(status -> {
			Subscription subscription = subscriptionRepository.findById(txResult.getSubscriptionId())
				.orElseThrow(() -> new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));
			SubscriptionInvoice invoice = subscriptionInvoiceRepository.findById(txResult.getInvoiceId())
				.orElseThrow(() -> new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));

			if (finalPaymentResult.isSuccess()) {
				// 첫 결제 성공 시 인보이스 상태 업데이트 및 구독 활성화 유지
				long earnedPoints = invoice.getBillingAmount() * invoice.getPointRewardRate() / 100;
				invoice.markAsSucceeded(earnedPoints, LocalDateTime.now());
				subscriptionInvoiceRepository.save(invoice);

				// 포인트 적립 연동
				if (earnedPoints > 0) {
					pointService.earnPoint(memberId, earnedPoints, invoice.getId(), PointSourceType.SUBSCRIPTION);
				}
			} else {
				// 첫 결제 실패 시 구독 즉시 해지(CANCELLED)로 처리하고 인보이스 상태도 실패(FAILED)로 기록
				subscription.cancel();
				subscriptionRepository.save(subscription);

				invoice.markAsFailed(finalPaymentResult.getFailureReason());
				subscriptionInvoiceRepository.save(invoice);
			}
		});

		if (!finalPaymentResult.isSuccess()) {
			throw new SubscriptionException(
				SubscriptionErrorCode.FIRST_PAYMENT_FAILED,
				"첫 결제 실패: " + finalPaymentResult.getFailureReason()
			);
		}

		// 조회는 별도 읽기용으로 진행
		Subscription subscription = subscriptionRepository.findById(txResult.getSubscriptionId())
			.orElseThrow(() -> new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));
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
	 * 스케줄러가 개별 구독 건에 대해 비관적 락을 획득하고 트랜잭션을 처리하기 위해 호출하는 메서드
	 */
	public void processBillingWithLock(Long subscriptionId, LocalDate today) {
		// 1. Transaction 1: Lock subscription, perform checks, and create PENDING invoice if valid
		PrepareBillingResult result = requiresNewTxTemplate.execute(status -> {
			Subscription subscription = subscriptionRepository.findByIdWithPessimisticLock(subscriptionId)
				.orElseThrow(() -> new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));

			// 결제 상태 더블 체크
			if (subscription.getStatus() != SubscriptionStatus.ACTIVE || subscription.getNextBillingDate().isAfter(today)) {
				return PrepareBillingResult.skipped();
			}

			// 결제 주기를 판단하는 날짜의 기준은 실행일(today)이 아니라 원래 해당 구독의 결제 예정일(subscription.getNextBillingDate())로 지정
			String billingPeriod = subscription.getNextBillingDate().format(DateTimeFormatter.ofPattern("yyyy-MM"));

			// 결제 기한이 오늘 이전(어제 이하)인 경우
			if (subscription.getNextBillingDate().isBefore(today)) {
				Optional<SubscriptionInvoice> existingInvoice = subscriptionInvoiceRepository.findBySubscriptionIdAndBillingPeriod(subscription.getId(), billingPeriod);
				if (existingInvoice.isPresent()) {
					if (existingInvoice.get().getStatus() == InvoiceStatus.FAILED) {
						// 발제문 정책: 정기 결제 실패 시에도 구독 상태를 유지함. 
						// 필요 시 여기서 재시도 횟수 제한 등의 미납 정책을 추가할 수 있으나, 현재는 즉시 해지를 방지함.
						// return PrepareBillingResult.cancelled(); // 기존의 즉시 해지 로직 주석 처리
					}
					// 이미 성공했거나 대기 중인 경우 스킵
					return PrepareBillingResult.skipped();
				}
			} else {
				// 오늘이 결제 예정일인 경우 중복 결제 방지
				if (subscriptionInvoiceRepository.existsBySubscriptionIdAndBillingPeriod(subscription.getId(), billingPeriod)) {
					return PrepareBillingResult.skipped();
				}
			}

			Plan plan = subscription.getPlan();
			PaymentMethod paymentMethod = subscription.getPaymentMethod();
			String portonePaymentId = "sub-sched-" + subscription.getId() + "-" + today.toString();

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

			return PrepareBillingResult.ready(
				subscription.getId(),
				invoice.getId(),
				paymentMethod.getPortoneBillingKey(),
				plan.getMonthlyAmount(),
				plan.getName()
			);
		});

		if (result == null || result.getStatus() != PrepareBillingResult.Status.READY) {
			return;
		}

		// 2. Call external PG API outside transaction with try-catch safety net
		SubscriptionPaymentService.PaymentResult paymentResult;
		try {
			paymentResult = subscriptionPaymentService.pay(
				result.getBillingKey(),
				result.getBillingAmount(),
				result.getPlanName()
			);
		} catch (Exception e) {
			paymentResult = SubscriptionPaymentService.PaymentResult.fail("PG 결제 API 호출 중 예외 발생: " + e.getMessage());
		}

		final SubscriptionPaymentService.PaymentResult finalPaymentResult = paymentResult;

		// 3. Transaction 2: Finalize billing status (Success/Failure)
		requiresNewTxTemplate.executeWithoutResult(status -> {
			Subscription subscription = subscriptionRepository.findById(result.getSubscriptionId())
				.orElseThrow(() -> new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));
			SubscriptionInvoice invoice = subscriptionInvoiceRepository.findById(result.getInvoiceId())
				.orElseThrow(() -> new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));

			if (finalPaymentResult.isSuccess()) {
				long earnedPoints = invoice.getBillingAmount() * invoice.getPointRewardRate() / 100;
				invoice.markAsSucceeded(earnedPoints, LocalDateTime.now());
				subscriptionInvoiceRepository.save(invoice);

				if (earnedPoints > 0) {
					pointService.earnPoint(subscription.getMemberId(), earnedPoints, invoice.getId(), PointSourceType.SUBSCRIPTION);
				}

				// 모든 미납 인보이스가 해결되었는지 확인
				boolean hasRemainingFailedInvoices = subscriptionInvoiceRepository.existsBySubscriptionIdAndStatus(subscription.getId(), InvoiceStatus.FAILED);
				if (!hasRemainingFailedInvoices) {
					subscription.clearUnpaid();
				}
				subscription.renewNextBillingDate();
				subscriptionRepository.save(subscription);
			} else {
				invoice.markAsFailed(finalPaymentResult.getFailureReason());
				subscriptionInvoiceRepository.save(invoice);

				// 발제문 정책 보완: 실패 시에도 다음 결제일로 넘기되 미납 상태로 전환
				subscription.markAsUnpaid();
				subscription.renewNextBillingDate();
				subscriptionRepository.save(subscription);
			}
		});
	}
}
