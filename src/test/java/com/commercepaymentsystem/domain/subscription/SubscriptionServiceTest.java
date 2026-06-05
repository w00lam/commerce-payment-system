package com.commercepaymentsystem.domain.subscription;

import com.commercepaymentsystem.domain.member.entity.Member;
import com.commercepaymentsystem.domain.member.repository.MemberRepository;
import com.commercepaymentsystem.domain.subscription.dto.RegisterPaymentMethodRequest;
import com.commercepaymentsystem.domain.subscription.dto.StartSubscriptionRequest;
import com.commercepaymentsystem.domain.subscription.dto.SubscriptionResponse;
import com.commercepaymentsystem.domain.subscription.entity.InvoiceStatus;
import com.commercepaymentsystem.domain.subscription.entity.PaymentMethod;
import com.commercepaymentsystem.domain.subscription.entity.Plan;
import com.commercepaymentsystem.domain.subscription.entity.Subscription;
import com.commercepaymentsystem.domain.subscription.entity.SubscriptionInvoice;
import com.commercepaymentsystem.domain.subscription.entity.SubscriptionStatus;
import com.commercepaymentsystem.domain.subscription.repository.PaymentMethodRepository;
import com.commercepaymentsystem.domain.subscription.repository.PlanRepository;
import com.commercepaymentsystem.domain.subscription.repository.SubscriptionInvoiceRepository;
import com.commercepaymentsystem.domain.subscription.repository.SubscriptionRepository;
import com.commercepaymentsystem.domain.subscription.service.SubscriptionService;
import com.commercepaymentsystem.domain.subscription.exception.SubscriptionException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest
class SubscriptionServiceTest {

	@Autowired
	private SubscriptionService subscriptionService;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private PlanRepository planRepository;

	@Autowired
	private PaymentMethodRepository paymentMethodRepository;

	@Autowired
	private SubscriptionRepository subscriptionRepository;

	@Autowired
	private SubscriptionInvoiceRepository subscriptionInvoiceRepository;

	@Autowired
	private com.commercepaymentsystem.domain.point.repository.PointHistoryRepository pointHistoryRepository;

	@Autowired
	private com.commercepaymentsystem.domain.membership.repository.MemberMembershipRepository memberMembershipRepository;

	private Member member;
	private Plan basicPlan;
	private PaymentMethod successPaymentMethod;
	private PaymentMethod failPaymentMethod;

	@org.junit.jupiter.api.AfterEach
	void tearDown() {
		subscriptionInvoiceRepository.deleteAllInBatch();
		subscriptionRepository.deleteAllInBatch();
		paymentMethodRepository.deleteAllInBatch();
		memberMembershipRepository.deleteAllInBatch();
		pointHistoryRepository.deleteAllInBatch();
		planRepository.deleteAllInBatch();
		memberRepository.deleteAllInBatch();
	}

	@BeforeEach
	void setUp() {
		member = memberRepository.save(Member.create(
			"test@example.com",
			"password123",
			"Hong Gil Dong",
			"010-1234-5678"
		));

		basicPlan = planRepository.save(new Plan("Test Basic Plan", 10000L, "Test Basic"));

		successPaymentMethod = paymentMethodRepository.save(new PaymentMethod(
			member.getId(),
			"SUCCESS_BILLING_KEY",
			"TossCard"
		));

		failPaymentMethod = paymentMethodRepository.save(new PaymentMethod(
			member.getId(),
			"FAIL_KEY",
			"TossCard"
		));
	}

	@Test
	void registerPaymentMethod_Success() {
		RegisterPaymentMethodRequest request = new RegisterPaymentMethodRequest("NEW_BILLING_KEY", "Kakaocard");
		PaymentMethod registered = subscriptionService.registerPaymentMethod(member.getId(), request);

		assertThat(registered.getId()).isNotNull();
		assertThat(registered.getPortoneBillingKey()).isEqualTo("NEW_BILLING_KEY");
		assertThat(registered.getCardCompanyName()).isEqualTo("Kakaocard");
	}

	@Test
	void registerPaymentMethod_Failure_AlreadyRegisteredByOtherMember() {
		Member otherMember = memberRepository.save(Member.create(
			"other@example.com",
			"password123",
			"Other User",
			"010-9999-8888"
		));
		RegisterPaymentMethodRequest request1 = new RegisterPaymentMethodRequest("DUPLICATE_KEY", "KakaoCard");
		subscriptionService.registerPaymentMethod(otherMember.getId(), request1);

		RegisterPaymentMethodRequest request2 = new RegisterPaymentMethodRequest("DUPLICATE_KEY", "TossCard");
		assertThatThrownBy(() -> subscriptionService.registerPaymentMethod(member.getId(), request2))
			.isInstanceOf(SubscriptionException.class)
			.hasMessageContaining("이미 다른 회원이 등록한 결제 수단");
	}

	@Test
	void startSubscription_Success_FirstPaymentSucceeds() {
		StartSubscriptionRequest request = new StartSubscriptionRequest(basicPlan.getId(), successPaymentMethod.getId());
		SubscriptionResponse response = subscriptionService.startSubscription(member.getId(), request);

		assertThat(response.getId()).isNotNull();
		assertThat(response.getPlanName()).isEqualTo(basicPlan.getName());
		assertThat(response.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
		assertThat(response.getNextBillingDate()).isEqualTo(LocalDate.now().plusMonths(1));

		// 인보이스가 성공(SUCCEEDED)으로 정상 발행되었는지 검증
		List<SubscriptionInvoice> invoices = subscriptionInvoiceRepository.findAllBySubscriptionId(response.getId());
		assertThat(invoices).hasSize(1);
		assertThat(invoices.get(0).getStatus()).isEqualTo(InvoiceStatus.SUCCEEDED);
		assertThat(invoices.get(0).getBillingAmount()).isEqualTo(10000L);

		// 포인트 적립 연동 검증 (기본 NORMAL 등급 1% 적립 = 100 포인트)
		Member updatedMember = memberRepository.findById(member.getId()).orElseThrow();
		assertThat(updatedMember.getPointBalance()).isEqualTo(100L);
	}

	@Test
	void startSubscription_Failure_FirstPaymentFails() {
		StartSubscriptionRequest request = new StartSubscriptionRequest(basicPlan.getId(), failPaymentMethod.getId());

		assertThatThrownBy(() -> subscriptionService.startSubscription(member.getId(), request))
			.isInstanceOf(SubscriptionException.class)
			.hasMessageContaining("첫 결제 실패");

		// 구독이 취소되었는지 검증
		List<Subscription> subscriptions = subscriptionRepository.findAll();
		assertThat(subscriptions).isNotEmpty();
		Subscription subscription = subscriptions.get(subscriptions.size() - 1);
		assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
		assertThat(subscription.getActivePlanKey()).isNull();

		// 인보이스 상태가 실패(FAILED)로 표기되었는지 검증 (noRollbackFor 설정으로 실패 기록 커밋됨)
		List<SubscriptionInvoice> invoices = subscriptionInvoiceRepository.findAllBySubscriptionId(subscription.getId());
		assertThat(invoices).hasSize(1);
		assertThat(invoices.get(0).getStatus()).isEqualTo(InvoiceStatus.FAILED);
		assertThat(invoices.get(0).getFailureReason()).contains("한도 초과");
	}

	@Test
	void cancelSubscription_Success() {
		StartSubscriptionRequest request = new StartSubscriptionRequest(basicPlan.getId(), successPaymentMethod.getId());
		SubscriptionResponse response = subscriptionService.startSubscription(member.getId(), request);

		subscriptionService.cancelSubscription(member.getId(), response.getId());

		Subscription subscription = subscriptionRepository.findById(response.getId()).orElseThrow();
		assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
		assertThat(subscription.getActivePlanKey()).isNull();
		assertThat(subscription.getCancelledAt()).isNotNull();
	}

	@Test
	void processBillingWithLock_Success() {
		Subscription subscription = Subscription.create(member.getId(), basicPlan, successPaymentMethod);
		// 다음 결제일을 오늘로 설정하여 결제 대상이 되도록 처리
		org.springframework.test.util.ReflectionTestUtils.setField(subscription, "nextBillingDate", LocalDate.now());
		subscriptionRepository.save(subscription);

		// 포인트 잔액 초기화 확인용
		long initialPoints = memberRepository.findById(member.getId()).orElseThrow().getPointBalance();

		LocalDate today = LocalDate.now();
		subscriptionService.processBillingWithLock(subscription.getId(), today);

		// 인보이스가 성공(SUCCEEDED)으로 생성되고 다음 결제일이 성공적으로 갱신되었는지 검증
		List<SubscriptionInvoice> invoices = subscriptionInvoiceRepository.findAllBySubscriptionId(subscription.getId());
		assertThat(invoices).hasSize(1);
		assertThat(invoices.get(0).getStatus()).isEqualTo(InvoiceStatus.SUCCEEDED);
		
		Subscription updatedSub = subscriptionRepository.findById(subscription.getId()).orElseThrow();
		assertThat(updatedSub.getNextBillingDate()).isEqualTo(subscription.getStartedAt().toLocalDate().plusMonths(1));

		// 포인트 추가 적립 검증 (1% = 100 포인트 적립)
		Member updatedMember = memberRepository.findById(member.getId()).orElseThrow();
		assertThat(updatedMember.getPointBalance()).isEqualTo(initialPoints + 100L);
	}

	@Test
	void processBillingWithLock_Failure() {
		Subscription subscription = Subscription.create(member.getId(), basicPlan, failPaymentMethod);
		// 다음 결제일을 오늘로 설정하여 결제 대상이 되도록 처리
		org.springframework.test.util.ReflectionTestUtils.setField(subscription, "nextBillingDate", LocalDate.now());
		subscriptionRepository.save(subscription);

		LocalDate today = LocalDate.now();
		subscriptionService.processBillingWithLock(subscription.getId(), today);

		// 인보이스가 실패(FAILED)로 생성되고 다음 결제일이 변하지 않았는지 검증
		List<SubscriptionInvoice> invoices = subscriptionInvoiceRepository.findAllBySubscriptionId(subscription.getId());
		assertThat(invoices).hasSize(1);
		assertThat(invoices.get(0).getStatus()).isEqualTo(InvoiceStatus.FAILED);
		
		Subscription updatedSub = subscriptionRepository.findById(subscription.getId()).orElseThrow();
		assertThat(updatedSub.getNextBillingDate()).isEqualTo(LocalDate.now());
	}

	@Test
	void processBillingWithLock_Overdue_CancelsSubscription() {
		// Given: 다음 결제일이 어제(오늘보다 이전)인 활성 구독
		Subscription subscription = Subscription.create(member.getId(), basicPlan, successPaymentMethod);
		subscriptionRepository.save(subscription);

		// 다음 결제일을 강제로 어제로 변경
		org.springframework.test.util.ReflectionTestUtils.setField(subscription, "nextBillingDate", LocalDate.now().minusDays(1));
		subscriptionRepository.save(subscription);

		// When: 오늘 날짜 기준으로 배치 처리 실행
		LocalDate today = LocalDate.now();
		subscriptionService.processBillingWithLock(subscription.getId(), today);

		// Then: 하루 경과로 정지/해지 처리 (CANCELLED)
		Subscription updatedSub = subscriptionRepository.findById(subscription.getId()).orElseThrow();
		assertThat(updatedSub.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);

		// 인보이스 발행 기록이 추가되지 않았음을 검증
		List<SubscriptionInvoice> invoices = subscriptionInvoiceRepository.findAllBySubscriptionId(subscription.getId());
		assertThat(invoices).isEmpty();
	}
}
