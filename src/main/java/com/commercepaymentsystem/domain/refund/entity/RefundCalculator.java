package com.commercepaymentsystem.domain.refund.entity;

import java.util.Collection;

import com.commercepaymentsystem.domain.refund.exception.RefundErrorCode;
import com.commercepaymentsystem.domain.refund.exception.RefundException;

public final class RefundCalculator {

	private RefundCalculator() {
	}

	public record RefundAmount(
		Long pointRefundAmount,
		Long pgRefundAmount
	) {

		public Long totalAmount() {
			return pointRefundAmount + pgRefundAmount;
		}
	}

	/**
	 * 환불 상세 항목들의 포인트/PG 환불 금액을 각각 합산하여 부분 환불 금액을 계산합니다.
	 */
	public static RefundAmount calculatePartialRefundAmount(Collection<RefundItem> refundItems) {
		if (refundItems == null || refundItems.isEmpty()) {
			throw new RefundException(RefundErrorCode.INVALID_AMOUNT);
		}

		long pointRefundAmount = refundItems.stream()
			.mapToLong(RefundItem::getPointRefundAmount)
			.sum();
		long pgRefundAmount = refundItems.stream()
			.mapToLong(RefundItem::getPgRefundAmount)
			.sum();

		return new RefundAmount(pointRefundAmount, pgRefundAmount);
	}

	/**
	 * 결제 시 사용한 포인트 금액과 PG 결제 금액에서 기존 환불 누적 금액을 제외하여 전체 환불 가능 금액을 계산합니다.
	 */
	public static RefundAmount calculateFullRefundAmount(
		Long pointAmount,
		Long pgAmount,
		Long alreadyPointRefundedAmount,
		Long alreadyPgRefundedAmount
	) {
		validateNotNegative(pointAmount);
		validateNotNegative(pgAmount);
		validateNotNegative(alreadyPointRefundedAmount);
		validateNotNegative(alreadyPgRefundedAmount);

		if (alreadyPointRefundedAmount > pointAmount || alreadyPgRefundedAmount > pgAmount) {
			throw new RefundException(RefundErrorCode.REFUND_AMOUNT_EXCEEDED);
		}

		return new RefundAmount(
			pointAmount - alreadyPointRefundedAmount,
			pgAmount - alreadyPgRefundedAmount
		);
	}

	/**
	 * 요청 환불 금액이 현재 남은 환불 가능 금액을 초과하지 않는지 검증합니다.
	 */
	public static void validateRefundableAmount(
		Long requestedPointRefundAmount,
		Long requestedPgRefundAmount,
		Long pointAmount,
		Long pgAmount,
		Long alreadyPointRefundedAmount,
		Long alreadyPgRefundedAmount
	) {
		validateNotNegative(requestedPointRefundAmount);
		validateNotNegative(requestedPgRefundAmount);

		RefundAmount remainingAmount = calculateFullRefundAmount(
			pointAmount,
			pgAmount,
			alreadyPointRefundedAmount,
			alreadyPgRefundedAmount
		);

		if (
			requestedPointRefundAmount + requestedPgRefundAmount <= 0 ||
			requestedPointRefundAmount > remainingAmount.pointRefundAmount() ||
			requestedPgRefundAmount > remainingAmount.pgRefundAmount()
		) {
			throw new RefundException(RefundErrorCode.REFUND_AMOUNT_EXCEEDED);
		}
	}

	private static void validateNotNegative(Long amount) {
		if (amount == null || amount < 0) {
			throw new RefundException(RefundErrorCode.INVALID_AMOUNT);
		}
	}
}
