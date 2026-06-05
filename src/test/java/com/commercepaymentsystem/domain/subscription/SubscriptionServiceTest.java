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
@Transactional
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

	private Member member;
	private Plan basicPlan;
	private PaymentMethod successPaymentMethod;
	private PaymentMethod failPaymentMethod;

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

		// 인보이스 상태가 실패(FAILED)로 표기되었는지 검증
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
	void executeBilling_Success() {
		Subscription subscription = Subscription.create(member.getId(), basicPlan, successPaymentMethod);
		subscriptionRepository.save(subscription);

		LocalDate today = LocalDate.now();
		subscriptionService.executeBilling(subscription, today);

		// 인보이스가 성공(SUCCEEDED)으로 생성되고 다음 결제일이 성공적으로 갱신되었는지 검증
		List<SubscriptionInvoice> invoices = subscriptionInvoiceRepository.findAllBySubscriptionId(subscription.getId());
		assertThat(invoices).hasSize(1);
		assertThat(invoices.get(0).getStatus()).isEqualTo(InvoiceStatus.SUCCEEDED);
		
		Subscription updatedSub = subscriptionRepository.findById(subscription.getId()).orElseThrow();
		assertThat(updatedSub.getNextBillingDate()).isEqualTo(subscription.getStartedAt().toLocalDate().plusMonths(2));
	}

	@Test
	void executeBilling_Failure() {
		Subscription subscription = Subscription.create(member.getId(), basicPlan, failPaymentMethod);
		subscriptionRepository.save(subscription);

		LocalDate today = LocalDate.now();
		subscriptionService.executeBilling(subscription, today);

		// 인보이스가 실패(FAILED)로 생성되고 다음 결제일이 변하지 않았는지 검증
		List<SubscriptionInvoice> invoices = subscriptionInvoiceRepository.findAllBySubscriptionId(subscription.getId());
		assertThat(invoices).hasSize(1);
		assertThat(invoices.get(0).getStatus()).isEqualTo(InvoiceStatus.FAILED);
		
		Subscription updatedSub = subscriptionRepository.findById(subscription.getId()).orElseThrow();
		assertThat(updatedSub.getNextBillingDate()).isEqualTo(subscription.getStartedAt().toLocalDate().plusMonths(1));
	}
}
