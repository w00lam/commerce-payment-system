package com.commercepaymentsystem.domain.subscription.entity;

import com.commercepaymentsystem.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "subscriptions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "plan_id", nullable = false)
	private Plan plan;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "payment_method_id", nullable = false)
	private PaymentMethod paymentMethod;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private SubscriptionStatus status;

	@Column(name = "active_plan_key", unique = true, length = 255)
	private String activePlanKey;

	@Column(name = "next_billing_date", nullable = false)
	private LocalDate nextBillingDate;

	@Column(name = "started_at", nullable = false)
	private LocalDateTime startedAt;

	@Column(name = "cancelled_at")
	private LocalDateTime cancelledAt;

	@Column(name = "is_unpaid", nullable = false)
	private boolean unpaid = false;

	public static Subscription create(Long memberId, Plan plan, PaymentMethod paymentMethod) {
		Subscription subscription = new Subscription();
		subscription.memberId = memberId;
		subscription.plan = plan;
		subscription.paymentMethod = paymentMethod;
		subscription.status = SubscriptionStatus.ACTIVE;
		subscription.activePlanKey = memberId + ":" + plan.getId();
		subscription.startedAt = LocalDateTime.now();
		
		// 최초 다음 결제일 설정 (가입일 기준 1개월 뒤)
		subscription.nextBillingDate = subscription.startedAt.toLocalDate().plusMonths(1);
		
		return subscription;
	}

	// 말일 클램핑 로직 테스트를 위한 public 생성자
	public Subscription(LocalDateTime startedAt, LocalDate nextBillingDate) {
		this.startedAt = startedAt;
		this.nextBillingDate = nextBillingDate;
	}

	/**
	 * 다음 결제일 갱신 (말일 클램프 로직 반영)
	 * 단순히 기존 nextBillingDate에서 plusMonths(1)을 하면 일자(day-of-month)가 밀리는 현상이 발생하므로,
	 * 최초 가입일(startedAt)을 기준으로 경과된 총 개월 수를 계산한 뒤 (N + 1) 개월을 더하여 일자를 복원/보존합니다.
	 */
	public void renewNextBillingDate() {
		int yearsDiff = this.nextBillingDate.getYear() - this.startedAt.getYear();
		int monthsDiff = this.nextBillingDate.getMonthValue() - this.startedAt.getMonthValue();
		int totalMonths = yearsDiff * 12 + monthsDiff;
		this.nextBillingDate = this.startedAt.toLocalDate().plusMonths(totalMonths + 1);
	}

	/**
	 * 구독 해지 처리
	 */
	public void cancel() {
		if (this.status == SubscriptionStatus.CANCELLED) {
			return;
		}
		this.status = SubscriptionStatus.CANCELLED;
		this.activePlanKey = null;
		this.cancelledAt = LocalDateTime.now();
	}

	public void markAsUnpaid() {
		this.unpaid = true;
	}

	public void clearUnpaid() {
		this.unpaid = false;
	}
}
