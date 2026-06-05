package com.commercepaymentsystem.domain.refund.entity;

import com.commercepaymentsystem.domain.refund.exception.RefundErrorCode;
import com.commercepaymentsystem.domain.refund.exception.RefundException;

public enum RefundStatus {

	REQUESTED,
	PROCESSING,
	COMPLETED,
	POST_PROCESS_FAILED,
	FAILED;

	/**
	 * 더 이상 상태 전이가 필요하지 않은 최종 상태인지 확인합니다.
	 */
	public boolean isTerminal() {
		return this == COMPLETED || this == POST_PROCESS_FAILED || this == FAILED;
	}

	/**
	 * 요청 상태의 환불을 처리 중 상태로 전환합니다.
	 */
	public RefundStatus startProcessing() {
		if (this != REQUESTED) {
			throw new RefundException(RefundErrorCode.INVALID_REFUND_STATUS);
		}

		return PROCESSING;
	}

	/**
	 * 처리 중 상태의 환불을 완료 상태로 전환합니다.
	 */
	public RefundStatus complete() {
		if (this != PROCESSING) {
			throw new RefundException(RefundErrorCode.INVALID_REFUND_STATUS);
		}

		return COMPLETED;
	}

	/**
	 * 요청 또는 처리 중 상태의 환불을 실패 상태로 전환합니다.
	 */
	public RefundStatus fail() {
		if (isTerminal()) {
			throw new RefundException(RefundErrorCode.INVALID_REFUND_STATUS);
		}

		return FAILED;
	}

	public RefundStatus failPostProcess() {
		// PG 취소 성공 후 내부 후처리가 실패한 경우에만 사용합니다.
		if (this != PROCESSING) {
			throw new RefundException(RefundErrorCode.INVALID_REFUND_STATUS);
		}

		return POST_PROCESS_FAILED;
	}
}
