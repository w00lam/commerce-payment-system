package com.commercepaymentsystem.domain.refund.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
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
@Table(name = "refunds")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Refund extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NonNull
	@Column(
		name = "payment_id",
		nullable = false
	)
	private Long paymentId;

	@NonNull
	@Column(
		name = "reason",
		nullable = false,
		length = 255
	)
	private String reason;

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

	@NonNull
	@Enumerated(EnumType.STRING)
	@Column(
		nullable = false,
		length = 30
	)
	private RefundStatus status;

	@OneToMany(mappedBy = "refund", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<RefundItem> items = new ArrayList<>();

	/**
	 * 현재 시각을 요청 시각으로 사용하여 환불 요청을 생성합니다.
	 *
	 * 환불은 항상 REQUESTED 상태로 시작하며, 필수 식별자와 금액, 사유가 유효하지 않으면 생성하지 않습니다.
	 */
	public static Refund create(
		Long paymentId,
		String reason,
		Long pointRefundAmount,
		Long pgRefundAmount
	) {
		validatePaymentId(paymentId);
		validateReason(reason);
		validateRefundAmounts(pointRefundAmount, pgRefundAmount);

		return new Refund(
			paymentId,
			reason,
			pointRefundAmount,
			pgRefundAmount,
			RefundStatus.REQUESTED
		);
	}

	public Long getTotalRefundAmount() {
		return this.pointRefundAmount + this.pgRefundAmount;
	}

	/**
	 * 환불 항목 목록을 외부에서 직접 변경하지 못하도록 읽기 전용 목록으로 반환합니다.
	 */
	public List<RefundItem> getItems() {
		return Collections.unmodifiableList(items);
	}

	/**
	 * 환불 요청에 상세 환불 항목을 추가하고 양방향 연관관계를 설정합니다.
	 */
	public void addItem(RefundItem item) {
		if (item == null) {
			throw new RefundException(RefundErrorCode.INVALID_AMOUNT);
		}

		item.assignRefund(this);
		this.items.add(item);
	}

	/**
	 * 환불 요청을 처리 중 상태로 전환합니다.
	 */
	public void startProcessing() {
		this.status = this.status.startProcessing();
	}

	/**
	 * 환불 처리를 완료 상태로 전환하고 완료 시각을 기록합니다.
	 */
	public void complete() {
		this.status = this.status.complete();
	}

	/**
	 * 환불 요청을 실패 상태로 전환합니다.
	 */
	public void fail() {
		this.status = this.status.fail();
	}

	public void failPostProcess() {
		this.status = this.status.failPostProcess();
	}

	private static void validatePaymentId(Long paymentId) {
		if (paymentId == null || paymentId <= 0) {
			throw new RefundException(RefundErrorCode.INVALID_PAYMENT_ID);
		}
	}

	private static void validateReason(String reason) {
		if (reason == null || reason.isBlank()) {
			throw new RefundException(RefundErrorCode.INVALID_REASON);
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
