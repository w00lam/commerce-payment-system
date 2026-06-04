package com.commercepaymentsystem.domain.refund.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.commercepaymentsystem.domain.payment.entity.Payment;
import com.commercepaymentsystem.domain.refund.dto.RefundCommand;
import com.commercepaymentsystem.domain.refund.dto.RefundItemCommand;
import com.commercepaymentsystem.domain.refund.entity.Refund;
import com.commercepaymentsystem.domain.refund.entity.RefundItem;
import com.commercepaymentsystem.domain.refund.port.RefundOrderPort.RefundableOrderInfo;
import com.commercepaymentsystem.domain.refund.port.RefundOrderPort.RefundableOrderInfo.RefundableOrderItemInfo;
import com.commercepaymentsystem.domain.refund.repository.RefundRepository;
import com.commercepaymentsystem.domain.refund.service.RefundService.PreparedRefund;

class RefundServiceTest {

	private final RefundRepository refundRepository = mock(RefundRepository.class);
	private final RefundService refundService = new RefundService(refundRepository);

	@Test
	@DisplayName("Processing refunds are not counted as already refunded quantities")
	void prepareRefund_processingRefund_notCountedAsRefunded() {
		Payment payment = confirmedPayment(10_000L, 2_000L, 8_000L);
		Refund processingRefund = refund(1000L, 1_000L, 4_000L);
		processingRefund.addItem(RefundItem.create(10L, 1L, 1_000L, 4_000L));
		processingRefund.startProcessing();
		when(refundRepository.findByPaymentId(100L)).thenReturn(List.of(processingRefund));
		when(refundRepository.save(any(Refund.class))).thenAnswer(invocation -> {
			Refund refund = invocation.getArgument(0);
			ReflectionTestUtils.setField(refund, "id", 1001L);
			return refund;
		});

		PreparedRefund preparedRefund = refundService.prepareRefund(
			new RefundCommand(
				"payment-123",
				1L,
				"retry request",
				List.of(new RefundItemCommand(10L, 2L))
			),
			payment,
			new RefundableOrderInfo(
				10L,
				1L,
				List.of(new RefundableOrderItemInfo(10L, 2L, 5_000L))
			)
		);

		assertThat(preparedRefund.refundId()).isEqualTo(1001L);
		assertThat(preparedRefund.pgAmount()).isEqualTo(8_000L);
		assertThat(preparedRefund.currentPgCancellableAmount()).isEqualTo(8_000L);
	}

	@Test
	@DisplayName("Post process failed refunds are counted because PortOne cancel already succeeded")
	void getRefundedAmounts_postProcessFailed_counted() {
		Refund completedRefund = refund(1000L, 500L, 2_000L);
		completedRefund.startProcessing();
		completedRefund.complete();
		Refund postProcessFailedRefund = refund(1001L, 300L, 1_200L);
		postProcessFailedRefund.startProcessing();
		postProcessFailedRefund.failPostProcess();
		Refund processingRefund = refund(1002L, 100L, 400L);
		processingRefund.startProcessing();
		Refund failedRefund = refund(1003L, 200L, 800L);
		failedRefund.fail();

		RefundService.RefundAmounts refundedAmounts = refundService.getRefundedAmounts(List.of(
			completedRefund,
			postProcessFailedRefund,
			processingRefund,
			failedRefund
		));

		assertThat(refundedAmounts.pointAmount()).isEqualTo(800L);
		assertThat(refundedAmounts.pgAmount()).isEqualTo(3_200L);
	}

	private Payment confirmedPayment(Long totalAmount, Long pointAmount, Long pgAmount) {
		Payment payment = Payment.create("payment-123", 1L, 10L, totalAmount, pointAmount, pgAmount);
		ReflectionTestUtils.setField(payment, "id", 100L);
		payment.confirm(Instant.parse("2026-06-01T01:02:03Z"));
		return payment;
	}

	private Refund refund(Long refundId, Long pointAmount, Long pgAmount) {
		Refund refund = Refund.create(100L, "refund", pointAmount, pgAmount);
		ReflectionTestUtils.setField(refund, "id", refundId);
		return refund;
	}
}
