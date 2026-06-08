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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
	name = "subscription_invoices",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_subscription_invoices_subscription_billing_period",
			columnNames = {"subscription_id", "billing_period"}
		)
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubscriptionInvoice extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "subscription_id", nullable = false)
	private Subscription subscription;

	@Column(name = "billing_period", nullable = false, length = 7)
	private String billingPeriod; // YYYY-MM

	@Column(name = "portone_payment_id", nullable = false, unique = true, length = 100)
	private String portonePaymentId;

	@Column(name = "billing_amount", nullable = false)
	private Long billingAmount;

	@Column(name = "membership_grade_name", nullable = false, length = 50)
	private String membershipGradeName;

	@Column(name = "point_reward_rate", nullable = false)
	private Integer pointRewardRate;

	@Column(name = "earned_point_amount", nullable = false)
	private Long earnedPointAmount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private InvoiceStatus status;

	@Column(name = "paid_at")
	private LocalDateTime paidAt;

	@Column(name = "failure_reason", length = 255)
	private String failureReason;

	public static SubscriptionInvoice createPending(
		Subscription subscription,
		String billingPeriod,
		String portonePaymentId,
		Long billingAmount,
		String membershipGradeName,
		Integer pointRewardRate
	) {
		SubscriptionInvoice invoice = new SubscriptionInvoice();
		invoice.subscription = subscription;
		invoice.billingPeriod = billingPeriod;
		invoice.portonePaymentId = portonePaymentId;
		invoice.billingAmount = billingAmount;
		invoice.membershipGradeName = membershipGradeName;
		invoice.pointRewardRate = pointRewardRate;
		invoice.earnedPointAmount = 0L;
		invoice.status = InvoiceStatus.PENDING;
		return invoice;
	}

	public void markAsSucceeded(String portonePaymentId, Long earnedPointAmount, LocalDateTime paidAt) {
		this.portonePaymentId = portonePaymentId;
		this.status = InvoiceStatus.SUCCEEDED;
		this.earnedPointAmount = earnedPointAmount;
		this.paidAt = paidAt;
	}

	public void markAsFailed(String portonePaymentId, String failureReason) {
		this.portonePaymentId = portonePaymentId;
		this.status = InvoiceStatus.FAILED;
		this.failureReason = failureReason;
	}
}
