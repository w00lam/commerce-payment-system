package com.commercepaymentsystem.domain.refund.facade;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;

import com.commercepaymentsystem.domain.payment.entity.Payment;
import com.commercepaymentsystem.domain.payment.entity.PaymentStatus;
import com.commercepaymentsystem.domain.payment.repository.PaymentRepository;
import com.commercepaymentsystem.domain.order.service.OrderNumberGenerator;
import com.commercepaymentsystem.domain.refund.dto.RefundCommand;
import com.commercepaymentsystem.domain.refund.dto.RefundItemCommand;
import com.commercepaymentsystem.domain.refund.dto.RefundResult;
import com.commercepaymentsystem.domain.refund.entity.Refund;
import com.commercepaymentsystem.domain.refund.entity.RefundItem;
import com.commercepaymentsystem.domain.refund.entity.RefundStatus;
import com.commercepaymentsystem.domain.refund.exception.RefundErrorCode;
import com.commercepaymentsystem.domain.refund.exception.RefundException;
import com.commercepaymentsystem.domain.refund.repository.RefundRepository;
import com.commercepaymentsystem.domain.refund.port.RefundOrderPort;
import com.commercepaymentsystem.domain.refund.port.RefundOrderPort.RefundableOrderInfo;
import com.commercepaymentsystem.domain.refund.port.RefundOrderPort.RefundableOrderInfo.RefundableOrderItemInfo;
import com.commercepaymentsystem.infrastructure.portone.client.PortOneClient;
import com.commercepaymentsystem.infrastructure.portone.dto.PortOnePaymentCancelRequest;
import com.commercepaymentsystem.infrastructure.portone.dto.PortOnePaymentCancelResponse;
import com.commercepaymentsystem.infrastructure.portone.exception.PortOneException;
import com.commercepaymentsystem.domain.payment.service.PaymentService;
import com.commercepaymentsystem.domain.refund.service.RefundPostProcessService;
import com.commercepaymentsystem.domain.refund.service.RefundService;

class RefundFacadeTest {

	private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
	private final RefundRepository refundRepository = mock(RefundRepository.class);
	private final PortOneClient portOneClient = mock(PortOneClient.class);
	private final RefundOrderPort refundOrderPort = mock(RefundOrderPort.class);
	private final RefundPostProcessService refundPostProcessService = mock(RefundPostProcessService.class);
	private final TransactionOperations transactionOperations = mock(TransactionOperations.class);

	private final PaymentService paymentService = new PaymentService(
		paymentRepository,
		null,
		new OrderNumberGenerator(),
		portOneClient
	);

	private final RefundService refundService = new RefundService(refundRepository);

	private final RefundFacade refundFacade = new RefundFacade(
		refundService,
		paymentService,
		refundOrderPort,
		refundPostProcessService,
		portOneClient,
		transactionOperations
	);

	@Test
	@DisplayName("Partial refund saves refund, calls PortOne with PG amount, and marks payment partially refunded")
	void refundPayment_partialRefund_success() {
		runTransactionsImmediately();
		Payment payment = confirmedPayment(10_000L, 2_000L, 8_000L);
		RefundableOrderInfo orderInfo = refundableOrderInfo(10L, 1L, itemInfo(10L, 2L, 5_000L));
		when(paymentRepository.findByPaymentIdForUpdate("payment-123")).thenReturn(Optional.of(payment));
		when(paymentRepository.findById(100L)).thenReturn(Optional.of(payment));
		when(refundOrderPort.getRefundableOrder(10L, 1L)).thenReturn(orderInfo);
		when(refundRepository.findByPaymentId(100L)).thenReturn(List.of());
		stubRefundSaveAndFind(1000L);
		when(portOneClient.cancelPayment(eq("payment-123"), any(PortOnePaymentCancelRequest.class)))
			.thenReturn(cancelResponse(4_000L));

		RefundResult result = refundFacade.refundPayment(
			new RefundCommand(
				"payment-123",
				1L,
				"customer request",
				List.of(new RefundItemCommand(10L, 1L))
			)
		);

		assertThat(result.refundId()).isEqualTo(1000L);
		assertThat(result.status()).isEqualTo(RefundStatus.COMPLETED);
		assertThat(result.pointRefundAmount()).isEqualTo(1_000L);
		assertThat(result.pgRefundAmount()).isEqualTo(4_000L);
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PARTIAL_REFUNDED);

		ArgumentCaptor<PortOnePaymentCancelRequest> requestCaptor =
			ArgumentCaptor.forClass(PortOnePaymentCancelRequest.class);
		verify(portOneClient).cancelPayment(eq("payment-123"), requestCaptor.capture());
		assertThat(requestCaptor.getValue().amount()).isEqualTo(4_000L);
		assertThat(requestCaptor.getValue().currentCancellableAmount()).isEqualTo(8_000L);
		verify(refundPostProcessService).process(
			eq(payment),
			eq(10L),
			argThat(refund -> refund.getId().equals(1000L) && refund.getStatus() == RefundStatus.COMPLETED),
			eq(false)
		);
	}

	@Test
	@DisplayName("Last remaining refund uses exact remaining payment method amounts and fully refunds payment")
	void refundPayment_fullRefund_success() {
		runTransactionsImmediately();
		Payment payment = confirmedPayment(10_000L, 2_000L, 8_000L);
		payment.markPartiallyRefunded();
		RefundableOrderInfo orderInfo = refundableOrderInfo(10L, 1L, itemInfo(10L, 2L, 5_000L));
		Refund previousRefund = completedRefund(100L, 1_000L, 4_000L);
		previousRefund.addItem(RefundItem.create(10L, 1L, 1_000L, 4_000L));
		when(paymentRepository.findByPaymentIdForUpdate("payment-123")).thenReturn(Optional.of(payment));
		when(paymentRepository.findById(100L)).thenReturn(Optional.of(payment));
		when(refundOrderPort.getRefundableOrder(10L, 1L)).thenReturn(orderInfo);
		when(refundRepository.findByPaymentId(100L)).thenReturn(List.of(previousRefund));
		stubRefundSaveAndFind(1001L);
		when(portOneClient.cancelPayment(eq("payment-123"), any(PortOnePaymentCancelRequest.class)))
			.thenReturn(cancelResponse(4_000L));

		RefundResult result = refundFacade.refundPayment(
			new RefundCommand(
				"payment-123",
				1L,
				"remaining refund",
				List.of(new RefundItemCommand(10L, 1L))
			)
		);

		assertThat(result.pointRefundAmount()).isEqualTo(1_000L);
		assertThat(result.pgRefundAmount()).isEqualTo(4_000L);
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
		verify(refundPostProcessService).process(
			eq(payment),
			eq(10L),
			argThat(refund -> refund.getId().equals(1001L) && refund.getStatus() == RefundStatus.COMPLETED),
			eq(true)
		);
	}

	@Test
	@DisplayName("Point-only refund skips PortOne cancel and runs post processing")
	void refundPayment_pointOnly_success() {
		runTransactionsImmediately();
		Payment payment = confirmedPayment(10_000L, 10_000L, 0L);
		RefundableOrderInfo orderInfo = refundableOrderInfo(10L, 1L, itemInfo(10L, 1L, 10_000L));
		when(paymentRepository.findByPaymentIdForUpdate("payment-123")).thenReturn(Optional.of(payment));
		when(refundOrderPort.getRefundableOrder(10L, 1L)).thenReturn(orderInfo);
		when(refundRepository.findByPaymentId(100L)).thenReturn(List.of());
		stubRefundSaveAndFind(1004L);

		RefundResult result = refundFacade.refundPayment(
			new RefundCommand(
				"payment-123",
				1L,
				"point refund",
				List.of(new RefundItemCommand(10L, 1L))
			)
		);

		assertThat(result.refundId()).isEqualTo(1004L);
		assertThat(result.status()).isEqualTo(RefundStatus.COMPLETED);
		assertThat(result.pointRefundAmount()).isEqualTo(10_000L);
		assertThat(result.pgRefundAmount()).isZero();
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
		verify(portOneClient, never()).cancelPayment(anyString(), any());
		verify(refundPostProcessService).process(
			eq(payment),
			eq(10L),
			argThat(refund -> refund.getId().equals(1004L) && refund.getStatus() == RefundStatus.COMPLETED),
			eq(true)
		);
	}

	@Test
	@DisplayName("Refund rejects quantity greater than remaining refundable quantity")
	void refundPayment_exceededQuantity_fail() {
		runTransactionsImmediately();
		Payment payment = confirmedPayment(10_000L, 2_000L, 8_000L);
		RefundableOrderInfo orderInfo = refundableOrderInfo(10L, 1L, itemInfo(10L, 2L, 5_000L));
		Refund previousRefund = completedRefund(100L, 1_000L, 4_000L);
		previousRefund.addItem(RefundItem.create(10L, 2L, 1_000L, 4_000L));
		when(paymentRepository.findByPaymentIdForUpdate("payment-123")).thenReturn(Optional.of(payment));
		when(refundOrderPort.getRefundableOrder(10L, 1L)).thenReturn(orderInfo);
		when(refundRepository.findByPaymentId(100L)).thenReturn(List.of(previousRefund));

		assertRefundException(
			() -> refundFacade.refundPayment(
				new RefundCommand(
					"payment-123",
					1L,
					"duplicate refund",
					List.of(new RefundItemCommand(10L, 1L))
				)
			),
			RefundErrorCode.REFUND_AMOUNT_EXCEEDED
		);
		verify(portOneClient, never()).cancelPayment(anyString(), any());
		verify(refundPostProcessService, never()).process(any(), any(), any(), anyBoolean());
	}

	@Test
	@DisplayName("Refund updates status to FAILED and does not restore stock/points when PortOne cancel fails")
	void refundPayment_portOneFailure_refundFailed() {
		runTransactionsImmediately();
		Payment payment = confirmedPayment(10_000L, 2_000L, 8_000L);
		RefundableOrderInfo orderInfo = refundableOrderInfo(10L, 1L, itemInfo(10L, 2L, 5_000L));
		when(paymentRepository.findByPaymentIdForUpdate("payment-123")).thenReturn(Optional.of(payment));
		when(refundOrderPort.getRefundableOrder(10L, 1L)).thenReturn(orderInfo);
		when(refundRepository.findByPaymentId(100L)).thenReturn(List.of());
		stubRefundSaveAndFind(1002L);
		when(portOneClient.cancelPayment(eq("payment-123"), any(PortOnePaymentCancelRequest.class)))
			.thenThrow(new PortOneException("cancel failed"));

		assertRefundException(
			() -> refundFacade.refundPayment(
				new RefundCommand(
					"payment-123",
					1L,
					"customer request",
					List.of(new RefundItemCommand(10L, 1L))
				)
			),
			RefundErrorCode.PORTONE_REFUND_FAILED
		);

		ArgumentCaptor<Refund> refundCaptor = ArgumentCaptor.forClass(Refund.class);
		verify(refundRepository).save(refundCaptor.capture());
		assertThat(refundCaptor.getValue().getStatus()).isEqualTo(RefundStatus.FAILED);
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
		verify(refundPostProcessService, never()).process(any(), any(), any(), anyBoolean());
	}

	@Test
	@DisplayName("Refund marks post process failure when internal completion fails after PortOne cancel succeeds")
	void refundPayment_postProcessFailure_afterPortOneSuccess_markPostProcessFailed() {
		runFirstTransactionAndFailSecondTransaction();
		Payment payment = confirmedPayment(10_000L, 2_000L, 8_000L);
		RefundableOrderInfo orderInfo = refundableOrderInfo(10L, 1L, itemInfo(10L, 2L, 5_000L));
		when(paymentRepository.findByPaymentIdForUpdate("payment-123")).thenReturn(Optional.of(payment));
		when(refundOrderPort.getRefundableOrder(10L, 1L)).thenReturn(orderInfo);
		when(refundRepository.findByPaymentId(100L)).thenReturn(List.of());
		stubRefundSaveAndFind(1003L);
		when(portOneClient.cancelPayment(eq("payment-123"), any(PortOnePaymentCancelRequest.class)))
			.thenReturn(cancelResponse(4_000L));

		assertThatThrownBy(() -> refundFacade.refundPayment(
			new RefundCommand(
				"payment-123",
				1L,
				"customer request",
				List.of(new RefundItemCommand(10L, 1L))
			)
		))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("post process failure");

		Refund refund = refundRepository.findById(1003L).orElseThrow();
		assertThat(refund.getStatus()).isEqualTo(RefundStatus.POST_PROCESS_FAILED);
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
		verify(portOneClient).cancelPayment(eq("payment-123"), any(PortOnePaymentCancelRequest.class));
	}

	private void runTransactionsImmediately() {
		when(transactionOperations.execute(any())).thenAnswer(invocation -> {
			TransactionCallback<?> callback = invocation.getArgument(0);
			return callback.doInTransaction(null);
		});
	}

	private void runFirstTransactionAndFailSecondTransaction() {
		AtomicInteger transactionCount = new AtomicInteger();
		when(transactionOperations.execute(any())).thenAnswer(invocation -> {
			int count = transactionCount.incrementAndGet();
			if (count == 2) {
				throw new IllegalStateException("post process failure");
			}

			TransactionCallback<?> callback = invocation.getArgument(0);
			return callback.doInTransaction(null);
		});
	}

	private void stubRefundSaveAndFind(Long refundId) {
		AtomicReference<Refund> savedRefund = new AtomicReference<>();
		when(refundRepository.save(any(Refund.class))).thenAnswer(invocation -> {
			Refund refund = invocation.getArgument(0);
			ReflectionTestUtils.setField(refund, "id", refundId);
			savedRefund.set(refund);
			return refund;
		});
		when(refundRepository.findById(refundId)).thenAnswer(invocation -> Optional.ofNullable(savedRefund.get()));
	}

	private Payment confirmedPayment(Long totalAmount, Long pointAmount, Long pgAmount) {
		Payment payment = Payment.create("payment-123", 1L, 10L, totalAmount, pointAmount, pgAmount);
		ReflectionTestUtils.setField(payment, "id", 100L);
		payment.confirm(Instant.parse("2026-06-01T01:02:03Z"));
		return payment;
	}

	private RefundableOrderInfo refundableOrderInfo(
		Long orderId,
		Long memberId,
		RefundableOrderItemInfo... orderItems
	) {
		return new RefundableOrderInfo(orderId, memberId, List.of(orderItems));
	}

	private RefundableOrderItemInfo itemInfo(Long orderItemId, Long quantity, Long orderPrice) {
		return new RefundableOrderItemInfo(orderItemId, quantity, orderPrice);
	}

	private Refund completedRefund(Long paymentId, Long pointAmount, Long pgAmount) {
		Refund refund = Refund.create(paymentId, "previous refund", pointAmount, pgAmount);
		refund.startProcessing();
		refund.complete();
		return refund;
	}

	private PortOnePaymentCancelResponse cancelResponse(Long amount) {
		return new PortOnePaymentCancelResponse(
			new PortOnePaymentCancelResponse.PortOnePaymentCancellation(
				"cancel-123",
				"SUCCEEDED",
				"pg-cancel-123",
				amount,
				"00",
				"approved",
				"customer request",
				Instant.parse("2026-06-01T01:03:03Z"),
				Instant.parse("2026-06-01T01:03:04Z")
			)
		);
	}

	private void assertRefundException(
		ThrowingCallable callable,
		RefundErrorCode expectedErrorCode
	) {
		assertThatThrownBy(callable)
			.isInstanceOf(RefundException.class)
			.extracting("errorCode")
			.isEqualTo(expectedErrorCode);
	}
}
