package com.commercepaymentsystem.domain.refund.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import com.commercepaymentsystem.domain.refund.exception.RefundErrorCode;
import com.commercepaymentsystem.domain.refund.exception.RefundException;
import com.commercepaymentsystem.global.entity.BaseEntity;

@Entity
@Getter
@Table(name = "refund_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class RefundItem extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "refund_id", nullable = false)
	private Refund refund;

	@NonNull
	@Column(
		name = "order_item_id",
		nullable = false
	)
	private Long orderItemId;

	@NonNull
	@Column(
		name = "refund_quantity",
		nullable = false
	)
	private Long refundQuantity;

	@NonNull
	@Column(
		name = "point_refund_amount",
		nullable = false
	)
	private Long pointRefundAmount;

	@NonNull
	@Column(
		name = "pg_refund_amount",
		nullable = false
	)
	private Long pgRefundAmount;

	/**
	 * 환불 대상 주문 상품 정보로 환불 상세 항목을 생성합니다.
	 *
	 * 항목별 환불 금액은 환불 수량과 단가를 곱해 계산합니다.
	 */
	public static RefundItem create(
		Long orderItemId,
		Long refundQuantity,
		Long pointRefundAmount,
		Long pgRefundAmount
	) {
		validatePositive(orderItemId);
		validatePositive(refundQuantity);
		validateRefundAmounts(pointRefundAmount, pgRefundAmount);

		return new RefundItem(
			orderItemId,
			refundQuantity,
			pointRefundAmount,
			pgRefundAmount
		);
	}

	/**
	 * Refund의 addItem 메서드에서만 호출하여 환불 요청과 상세 항목의 연관관계를 맞춥니다.
	 */
	void assignRefund(Refund refund) {
		this.refund = refund;
	}

	private static void validatePositive(Long value) {
		if (value == null || value <= 0) {
			throw new RefundException(RefundErrorCode.INVALID_AMOUNT);
		}
	}

	private static void validateRefundAmounts(Long pointRefundAmount, Long pgRefundAmount) {
		if (
			pointRefundAmount == null ||
			pgRefundAmount == null ||
			pointRefundAmount < 0 ||
			pgRefundAmount < 0 ||
			pointRefundAmount + pgRefundAmount <= 0
		) {
			throw new RefundException(RefundErrorCode.INVALID_AMOUNT);
		}
	}
}
