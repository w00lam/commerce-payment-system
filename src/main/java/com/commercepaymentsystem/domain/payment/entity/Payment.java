package com.commercepaymentsystem.domain.payment.entity;

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
	@Column(name = "total_order_amount", nullable = false)
	private Long totalOrderAmount;

	@NonNull
	@Column(name = "used_point_amount", nullable = false)
	private Long usedPointAmount;

	@NonNull
	@Column(name = "final_payment_amount", nullable = false)
	private Long finalPaymentAmount;

	@NonNull
	@Enumerated(EnumType.STRING)
	@Column(
		nullable = false,
		length = 30
	)
	private PaymentStatus status;

	public static Payment create(
		String paymentId,
		Long memberId,
		Long orderId,
		Long totalOrderAmount,
		Long usedPointAmount,
		Long finalPaymentAmount
	) {
		return new Payment(
			paymentId,
			memberId,
			orderId,
			totalOrderAmount,
			usedPointAmount,
			finalPaymentAmount,
			PaymentStatus.PENDING
		);
	}
}
