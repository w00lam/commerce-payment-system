package com.commercepaymentsystem.domain.payment.entity;

public enum PaymentStatus {

	PENDING,
	CONFIRMED,
	FAILED,
	PARTIAL_REFUNDED,
	REFUNDED;

	/**
	 * 현재 상태가 이미 확정 완료 상태인지 확인합니다.
	 */
	public boolean isConfirmed() {
		return this == CONFIRMED;
	}

	public boolean isRefundable() {
		return this == CONFIRMED || this == PARTIAL_REFUNDED;
	}

	/**
	 * 현재 상태에서 결제 확정 상태로 전이할 수 있는지 확인합니다.
	 *
	 * 현재 정책에서는 결제 대기 상태만 확정할 수 있습니다.
	 */
	public boolean isConfirmable() {
		return this == PENDING;
	}

	/**
	 * 현재 상태에서 결제 확정 후 상태를 반환합니다.
	 *
	 * 확정 불가능한 상태라면 기존 상태를 그대로 반환하고, 예외 처리는 호출 계층에서 담당합니다.
	 */
	public PaymentStatus confirm() {
		if (!isConfirmable()) {
			return this;
		}

		return CONFIRMED;
	}

	public PaymentStatus partialRefund() {
		if (!isRefundable()) {
			return this;
		}

		return PARTIAL_REFUNDED;
	}

	public PaymentStatus refund() {
		if (!isRefundable()) {
			return this;
		}

		return REFUNDED;
	}
}
