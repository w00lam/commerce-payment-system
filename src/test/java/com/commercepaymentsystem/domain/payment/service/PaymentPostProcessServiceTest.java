package com.commercepaymentsystem.domain.payment.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

import com.commercepaymentsystem.domain.payment.entity.Payment;
import com.commercepaymentsystem.domain.payment.port.CartPort;
import com.commercepaymentsystem.domain.payment.port.OrderPort;
import com.commercepaymentsystem.domain.payment.port.PointPort;

class PaymentPostProcessServiceTest {

	private final OrderPort orderPort = mock(OrderPort.class);
	private final PointPort pointPort = mock(PointPort.class);
	private final CartPort cartPort = mock(CartPort.class);

	private final PaymentPostProcessService paymentPostProcessService = new PaymentPostProcessService(
		orderPort,
		pointPort,
		cartPort
	);

	@Test
	@DisplayName("Payment post processing calls order, point, and cart ports in order")
	void process_success() {
		Payment payment = payment(2_000L, 8_000L);
		when(orderPort.confirmOrder(10L, 1L))
			.thenReturn(new OrderPort.ConfirmedOrder(List.of(100L, 101L)));

		paymentPostProcessService.process(payment);

		InOrder inOrder = inOrder(orderPort, pointPort, cartPort);
		inOrder.verify(orderPort).confirmOrder(10L, 1L);
		inOrder.verify(pointPort).deductUsedPoint(1L, 2_000L, 100L);
		inOrder.verify(pointPort).earnPoint(1L, 80L, 100L);
		inOrder.verify(cartPort).deleteOrderedCartItems(1L, List.of(100L, 101L));
	}

	@Test
	@DisplayName("Payment post processing skips zero point and empty cart operations")
	void process_zeroAmountsAndEmptyCart_skipOptionalCalls() {
		Payment payment = payment(0L, 0L);
		when(orderPort.confirmOrder(10L, 1L))
			.thenReturn(new OrderPort.ConfirmedOrder(List.of()));

		paymentPostProcessService.process(payment);

		verify(orderPort).confirmOrder(10L, 1L);
		verify(pointPort, never()).deductUsedPoint(any(), any(), any());
		verify(pointPort, never()).earnPoint(any(), any(), any());
		verify(cartPort, never()).deleteOrderedCartItems(any(), anyList());
	}

	@Test
	@DisplayName("Payment post processing propagates failures for transaction rollback")
	void process_pointFailure_propagatesException() {
		Payment payment = payment(2_000L, 8_000L);
		when(orderPort.confirmOrder(10L, 1L))
			.thenReturn(new OrderPort.ConfirmedOrder(List.of(100L)));
		doThrow(new IllegalStateException("point failure"))
			.when(pointPort).deductUsedPoint(1L, 2_000L, 100L);

		assertThatThrownBy(() -> paymentPostProcessService.process(payment))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("point failure");

		verify(cartPort, never()).deleteOrderedCartItems(any(), anyList());
	}

	private Payment payment(Long usedPointAmount, Long finalPaymentAmount) {
		Payment payment = Payment.create(
			"payment-123",
			1L,
			10L,
			usedPointAmount + finalPaymentAmount,
			usedPointAmount,
			finalPaymentAmount
		);
		ReflectionTestUtils.setField(payment, "id", 100L);
		return payment;
	}
}
