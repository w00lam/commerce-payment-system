package com.commercepaymentsystem.domain.refund.entity;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.assertj.core.api.ThrowableAssert;

import com.commercepaymentsystem.domain.refund.exception.RefundErrorCode;
import com.commercepaymentsystem.domain.refund.exception.RefundException;

class RefundTest {

	@Test
	@DisplayName("Refund creation succeeds with requested status")
	void createRefund_success() {
		Refund refund = Refund.create(
			100L,
			"Customer request",
			2_000L,
			8_000L
		);

		assertThat(refund.getPaymentId()).isEqualTo(100L);
		assertThat(refund.getReason()).isEqualTo("Customer request");
		assertThat(refund.getPointRefundAmount()).isEqualTo(2_000L);
		assertThat(refund.getPgRefundAmount()).isEqualTo(8_000L);
		assertThat(refund.getTotalRefundAmount()).isEqualTo(10_000L);
		assertThat(refund.getStatus()).isEqualTo(RefundStatus.REQUESTED);
	}

	@Test
	@DisplayName("Refund item is added with calculated amount")
	void addItem_success() {
		Refund refund = refund();
		RefundItem item = RefundItem.create(10L, 2L, 1_000L, 5_000L);

		refund.addItem(item);

		assertThat(refund.getItems()).hasSize(1);
		assertThat(item.getRefund()).isEqualTo(refund);
		assertThat(item.getOrderItemId()).isEqualTo(10L);
		assertThat(item.getRefundQuantity()).isEqualTo(2L);
		assertThat(item.getPointRefundAmount()).isEqualTo(1_000L);
		assertThat(item.getPgRefundAmount()).isEqualTo(5_000L);
	}

	@Test
	@DisplayName("Partial refund amount is sum of item refund amounts")
	void calculatePartialRefundAmount_success() {
		List<RefundItem> items = List.of(
			RefundItem.create(10L, 2L, 1_000L, 5_000L),
			RefundItem.create(11L, 1L, 500L, 3_500L)
		);

		RefundCalculator.RefundAmount amount = RefundCalculator.calculatePartialRefundAmount(items);

		assertThat(amount.pointRefundAmount()).isEqualTo(1_500L);
		assertThat(amount.pgRefundAmount()).isEqualTo(8_500L);
		assertThat(amount.totalAmount()).isEqualTo(10_000L);
	}

	@Test
	@DisplayName("Full refund amount considers payment amount, points, and previous refunds")
	void calculateFullRefundAmount_success() {
		RefundCalculator.RefundAmount amount = RefundCalculator.calculateFullRefundAmount(
			2_000L,
			8_000L,
			500L,
			3_000L
		);

		assertThat(amount.pointRefundAmount()).isEqualTo(1_500L);
		assertThat(amount.pgRefundAmount()).isEqualTo(5_000L);
		assertThat(amount.totalAmount()).isEqualTo(6_500L);
	}

	@Test
	@DisplayName("Refund status can move from requested to processing to completed")
	void statusTransition_success() {
		Refund refund = refund();

		refund.startProcessing();
		refund.complete();

		assertThat(refund.getStatus()).isEqualTo(RefundStatus.COMPLETED);
	}

	@Test
	@DisplayName("Completing requested refund directly fails")
	void completeWithoutProcessing_fail() {
		assertRefundException(
			() -> refund().complete(),
			RefundErrorCode.INVALID_REFUND_STATUS
		);
	}

	@Test
	@DisplayName("Duplicate or exceeded refund amount fails validation")
	void validateRefundableAmount_exceeded_fail() {
		assertRefundException(
			() -> RefundCalculator.validateRefundableAmount(2_001L, 8_000L, 2_000L, 8_000L, 0L, 0L),
			RefundErrorCode.REFUND_AMOUNT_EXCEEDED
		);
	}

	@Test
	@DisplayName("Invalid refund amount fails creation")
	void createRefund_invalidAmount_fail() {
		assertRefundException(
			() -> Refund.create(100L, "Customer request", 0L, 0L),
			RefundErrorCode.INVALID_AMOUNT
		);
	}

	private Refund refund() {
		return Refund.create(100L, "Customer request", 2_000L, 8_000L);
	}

	private void assertRefundException(
		ThrowableAssert.ThrowingCallable callable,
		RefundErrorCode expectedErrorCode
	) {
		assertThatThrownBy(callable)
			.isInstanceOf(RefundException.class)
			.extracting("errorCode")
			.isEqualTo(expectedErrorCode);
	}
}
