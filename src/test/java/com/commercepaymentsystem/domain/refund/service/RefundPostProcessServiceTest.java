package com.commercepaymentsystem.domain.refund.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

import com.commercepaymentsystem.domain.payment.entity.Payment;
import com.commercepaymentsystem.domain.refund.entity.Refund;
import com.commercepaymentsystem.domain.refund.entity.RefundItem;
import com.commercepaymentsystem.domain.refund.port.RefundMembershipPort;
import com.commercepaymentsystem.domain.refund.port.RefundOrderPort;
import com.commercepaymentsystem.domain.refund.port.RefundPointPort;
import com.commercepaymentsystem.domain.refund.port.RefundProductPort;

class RefundPostProcessServiceTest {

	private final RefundOrderPort refundOrderPort = mock(RefundOrderPort.class);
	private final RefundPointPort refundPointPort = mock(RefundPointPort.class);
	private final RefundProductPort refundProductPort = mock(RefundProductPort.class);
	private final RefundMembershipPort refundMembershipPort = mock(RefundMembershipPort.class);

	private final RefundPostProcessService refundPostProcessService = new RefundPostProcessService(
		refundOrderPort,
		refundPointPort,
		refundProductPort,
		refundMembershipPort
	);

	@Test
	@DisplayName("Partial refund restores stock and points and revokes earned points by refund ratio")
	void process_partialRefund_success() {
		Payment payment = confirmedPayment(10_000L, 2_000L, 8_000L);
		Refund refund = refund(1000L, 1_000L, 4_000L, RefundItem.create(10L, 1L, 1_000L, 4_000L));
		when(refundOrderPort.restoreProductStock(10L, Map.of(10L, 1L)))
			.thenReturn(Map.of(20L, 1L));

		refundPostProcessService.process(payment, 10L, refund, false);

		InOrder inOrder = inOrder(refundOrderPort, refundProductPort, refundPointPort, refundMembershipPort);
		inOrder.verify(refundOrderPort).restoreProductStock(10L, Map.of(10L, 1L));
		inOrder.verify(refundProductPort).restoreProductStocks(Map.of(20L, 1L));
		inOrder.verify(refundPointPort).restorePoint(1L, 1_000L, 100L, 1000L);
		inOrder.verify(refundPointPort).revokeEarnedPoint(1L, 40L, 100L, 1000L);
		inOrder.verify(refundMembershipPort).applyRefund(1L, 4_000L);
		verify(refundPointPort, never()).getRevokedEarnedPointAmount(anyLong());
		verify(refundOrderPort, never()).cancelOrder(any());
	}

	@Test
	@DisplayName("Full refund revokes remaining earned points and cancels order")
	void process_fullRefund_success() {
		Payment payment = confirmedPayment(10_000L, 2_000L, 8_000L);
		Refund refund = refund(1001L, 1_000L, 4_000L, RefundItem.create(10L, 1L, 1_000L, 4_000L));
		when(refundOrderPort.restoreProductStock(10L, Map.of(10L, 1L)))
			.thenReturn(Map.of(20L, 1L));
		when(refundPointPort.getRevokedEarnedPointAmount(100L)).thenReturn(40L);

		refundPostProcessService.process(payment, 10L, refund, true);

		verify(refundPointPort).revokeEarnedPoint(1L, 40L, 100L, 1001L);
		verify(refundMembershipPort).applyRefund(1L, 4_000L);
		verify(refundOrderPort).cancelOrder(10L);
	}

	@Test
	@DisplayName("Post processing skips zero point operations")
	void process_zeroPointAmounts_skipPointCalls() {
		Payment payment = confirmedPayment(10_000L, 0L, 10_000L);
		Refund refund = refund(1002L, 0L, 5_000L, RefundItem.create(10L, 1L, 0L, 5_000L));
		when(refundOrderPort.restoreProductStock(10L, Map.of(10L, 1L)))
			.thenReturn(Map.of(20L, 1L));

		refundPostProcessService.process(payment, 10L, refund, false);

		verify(refundPointPort, never()).restorePoint(anyLong(), anyLong(), anyLong(), anyLong());
		verify(refundPointPort).revokeEarnedPoint(1L, 50L, 100L, 1002L);
		verify(refundMembershipPort).applyRefund(1L, 5_000L);
	}

	@Test
	@DisplayName("Post processing propagates product restore failure for transaction rollback")
	void process_productFailure_propagatesException() {
		Payment payment = confirmedPayment(10_000L, 2_000L, 8_000L);
		Refund refund = refund(1003L, 1_000L, 4_000L, RefundItem.create(10L, 1L, 1_000L, 4_000L));
		when(refundOrderPort.restoreProductStock(10L, Map.of(10L, 1L)))
			.thenReturn(Map.of(20L, 1L));
		doThrow(new IllegalStateException("product failure"))
			.when(refundProductPort).restoreProductStocks(Map.of(20L, 1L));

		assertThatThrownBy(() -> refundPostProcessService.process(payment, 10L, refund, false))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("product failure");

		verify(refundPointPort, never()).restorePoint(anyLong(), anyLong(), anyLong(), anyLong());
		verify(refundPointPort, never()).revokeEarnedPoint(anyLong(), anyLong(), anyLong(), anyLong());
		verify(refundMembershipPort, never()).applyRefund(anyLong(), anyLong());
	}

	private Payment confirmedPayment(Long totalAmount, Long pointAmount, Long pgAmount) {
		Payment payment = Payment.create("payment-123", 1L, 10L, totalAmount, pointAmount, pgAmount);
		ReflectionTestUtils.setField(payment, "id", 100L);
		payment.confirm(Instant.parse("2026-06-01T01:02:03Z"));
		return payment;
	}

	private Refund refund(Long refundId, Long pointAmount, Long pgAmount, RefundItem refundItem) {
		Refund refund = Refund.create(100L, "refund", pointAmount, pgAmount);
		ReflectionTestUtils.setField(refund, "id", refundId);
		refund.addItem(refundItem);
		refund.startProcessing();
		refund.complete();
		return refund;
	}
}
