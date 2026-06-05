package com.commercepaymentsystem.domain.payment.entity;

import java.time.Instant;

import com.commercepaymentsystem.global.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Entity
@Getter
@Table(name = "payments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Payment extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NonNull
	@Column(name = "payment_id", nullable = false, unique = true, length = 100)
	private String paymentId;

	@NonNull
	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@NonNull
	@Column(name = "order_id", nullable = false)
	private Long orderId;

	@NonNull
	@Column(name = "order_name", length = 100)
	private String orderName;

	@NonNull
	@Column(name = "total_order_amount", nullable = false)
	private Long totalOrderAmount;

	@NonNull
	@Column(name = "used_point_amount", nullable = false)
	private Long usedPointAmount;

	@NonNull
	@Column(name = "final_payment_amount", nullable = false)
	private Long finalPaymentAmount;

	@NonNull
	@Column(name = "earned_point_amount", nullable = false)
	private Long earnedPointAmount;

	@NonNull
	@Enumerated(EnumType.STRING)
	@Column(
		nullable = false,
		length = 30
	)
	private PaymentStatus status;

	// 결제 확정이 성공한 시각입니다. PortOne 승인 시각 또는 서버 처리 시각을 저장합니다.
	@Column(name = "paid_at")
	private Instant paidAt;

	public static Payment create(
		String paymentId,
		Long memberId,
		Long orderId,
		String orderName,
		Long totalOrderAmount,
		Long usedPointAmount,
		Long finalPaymentAmount
	) {
		return new Payment(
			paymentId,
			memberId,
			orderId,
			orderName,
			totalOrderAmount,
			usedPointAmount,
			finalPaymentAmount,
			calculateEarnedPointAmount(finalPaymentAmount),
			PaymentStatus.PENDING
		);
	}

	public static Payment create(
		String paymentId,
		Long memberId,
		Long orderId,
		Long totalOrderAmount,
		Long usedPointAmount,
		Long finalPaymentAmount
	) {
		return create(
			paymentId,
			memberId,
			orderId,
			"order-" + orderId,
			totalOrderAmount,
			usedPointAmount,
			finalPaymentAmount
		);
	}

	private static Long calculateEarnedPointAmount(Long finalPaymentAmount) {
		if (finalPaymentAmount == null || finalPaymentAmount <= 0) {
			return 0L;
		}

		return finalPaymentAmount / 100;
	}

	/**
	 * 결제가 이미 확정 상태인지 확인합니다.
	 */
	public boolean isConfirmed() {
		return this.status.isConfirmed();
	}

	/**
	 * 결제가 확정 가능한 상태인지 확인합니다.
	 */
	public boolean isConfirmable() {
		return this.status.isConfirmable();
	}

	public boolean isRefundable() {
		return this.status.isRefundable();
	}

	/**
	 * 결제를 확정 상태로 변경하고 확정 시각을 저장합니다.
	 *
	 * 상태 전이 가능 여부와 다음 상태 결정은 {@link PaymentStatus}에 위임합니다.
	 */
	public void confirm(Instant paidAt) {
		this.status = this.status.confirm();
		this.paidAt = paidAt;
	}

	public void markPartiallyRefunded() {
		this.status = this.status.partialRefund();
	}

	public void markRefunded() {
		this.status = this.status.refund();
	}

	public boolean isPending() {
		return this.status.isPending();
	}

	public void fail() {
		this.status = this.status.fail();
	}
}
