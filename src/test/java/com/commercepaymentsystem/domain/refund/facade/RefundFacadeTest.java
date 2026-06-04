package com.commercepaymentsystem.domain.refund.facade;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;

import com.commercepaymentsystem.domain.member.entity.Member;
import com.commercepaymentsystem.domain.order.entity.Order;
import com.commercepaymentsystem.domain.order.entity.OrderItem;
import com.commercepaymentsystem.domain.order.entity.OrderStatus;
import com.commercepaymentsystem.domain.order.repository.OrderRepository;
import com.commercepaymentsystem.domain.payment.entity.Payment;
import com.commercepaymentsystem.domain.payment.entity.PaymentStatus;
import com.commercepaymentsystem.domain.payment.repository.PaymentRepository;
import com.commercepaymentsystem.domain.point.service.PointService;
import com.commercepaymentsystem.domain.product.entity.Product;
import com.commercepaymentsystem.domain.product.entity.ProductCategory;
import com.commercepaymentsystem.domain.product.entity.ProductStatus;
import com.commercepaymentsystem.domain.product.service.ProductService;
import com.commercepaymentsystem.domain.refund.dto.RefundCommand;
import com.commercepaymentsystem.domain.refund.dto.RefundItemCommand;
import com.commercepaymentsystem.domain.refund.dto.RefundResult;
import com.commercepaymentsystem.domain.refund.entity.Refund;
import com.commercepaymentsystem.domain.refund.entity.RefundItem;
import com.commercepaymentsystem.domain.refund.entity.RefundStatus;
import com.commercepaymentsystem.domain.refund.exception.RefundErrorCode;
import com.commercepaymentsystem.domain.refund.exception.RefundException;
import com.commercepaymentsystem.domain.refund.repository.RefundRepository;
import com.commercepaymentsystem.infrastructure.portone.client.PortOneClient;
import com.commercepaymentsystem.infrastructure.portone.dto.PortOnePaymentCancelRequest;
import com.commercepaymentsystem.infrastructure.portone.dto.PortOnePaymentCancelResponse;
import com.commercepaymentsystem.infrastructure.portone.exception.PortOneException;
import com.commercepaymentsystem.domain.payment.service.PaymentPostProcessService;
import com.commercepaymentsystem.domain.payment.service.PaymentService;
import com.commercepaymentsystem.domain.order.service.OrderService;
import com.commercepaymentsystem.domain.refund.service.RefundService;

class RefundFacadeTest {

	private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
	private final OrderRepository orderRepository = mock(OrderRepository.class);
	private final RefundRepository refundRepository = mock(RefundRepository.class);
	private final PortOneClient portOneClient = mock(PortOneClient.class);
	private final PointService pointService = mock(PointService.class);
	private final ProductService productService = mock(ProductService.class);
	private final PaymentPostProcessService paymentPostProcessService = mock(PaymentPostProcessService.class);
	private final TransactionOperations transactionOperations = mock(TransactionOperations.class);

	private final PaymentService paymentService = new PaymentService(
		paymentRepository,
		null,
		portOneClient
	);

	private final OrderService orderService = new OrderService(
		orderRepository,
		null
	);

	private final RefundService refundService = new RefundService(refundRepository);

	private final RefundFacade refundFacade = new RefundFacade(
		refundService,
		paymentService,
		orderService,
		pointService,
		productService,
		portOneClient,
		transactionOperations
	);

	@Test
	@DisplayName("Partial refund saves refund, calls PortOne with PG amount, and marks payment partially refunded")
	void refundPayment_partialRefund_success() {
		runTransactionsImmediately();
		Payment payment = confirmedPayment(10_000L, 2_000L, 8_000L);
		Order order = confirmedOrder(1L, orderItem(10L, 5_000L, 2L, 0L));
		when(paymentRepository.findByPaymentIdForUpdate("payment-123")).thenReturn(Optional.of(payment));
		when(paymentRepository.findById(100L)).thenReturn(Optional.of(payment));
		when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
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
		assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);

		ArgumentCaptor<PortOnePaymentCancelRequest> requestCaptor =
			ArgumentCaptor.forClass(PortOnePaymentCancelRequest.class);
		verify(portOneClient).cancelPayment(eq("payment-123"), requestCaptor.capture());
		assertThat(requestCaptor.getValue().amount()).isEqualTo(4_000L);
		assertThat(requestCaptor.getValue().currentCancellableAmount()).isEqualTo(8_000L);
		verify(pointService).restorePoint(1L, 1_000L, 100L, 1000L);
		verify(pointService).revokeEarnedPoint(1L, 40L, 100L, 1000L);
		verify(productService).restoreProductStocks(Map.of(10L, 1L));
	}

	@Test
	@DisplayName("Last remaining refund uses exact remaining payment method amounts and fully refunds payment")
	void refundPayment_fullRefund_success() {
		runTransactionsImmediately();
		Payment payment = confirmedPayment(10_000L, 2_000L, 8_000L);
		payment.markPartiallyRefunded();
		Order order = confirmedOrder(1L, orderItem(10L, 5_000L, 2L, 0L));
		Refund previousRefund = completedRefund(100L, 1_000L, 4_000L);
		previousRefund.addItem(RefundItem.create(10L, 1L, 1_000L, 4_000L));
		when(paymentRepository.findByPaymentIdForUpdate("payment-123")).thenReturn(Optional.of(payment));
		when(paymentRepository.findById(100L)).thenReturn(Optional.of(payment));
		when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
		when(refundRepository.findByPaymentId(100L)).thenReturn(List.of(previousRefund));
		stubRefundSaveAndFind(1001L);
		when(pointService.getRevokedEarnedPointAmount(100L)).thenReturn(40L);
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
		assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
		verify(productService).restoreProductStocks(Map.of(10L, 1L));
		verify(pointService).revokeEarnedPoint(1L, 40L, 100L, 1001L);
	}

	@Test
	@DisplayName("Refund rejects quantity greater than remaining refundable quantity")
	void refundPayment_exceededQuantity_fail() {
		runTransactionsImmediately();
		Payment payment = confirmedPayment(10_000L, 2_000L, 8_000L);
		Order order = confirmedOrder(1L, orderItem(10L, 5_000L, 2L, 0L));
		Refund previousRefund = completedRefund(100L, 1_000L, 4_000L);
		previousRefund.addItem(RefundItem.create(10L, 2L, 1_000L, 4_000L));
		when(paymentRepository.findByPaymentIdForUpdate("payment-123")).thenReturn(Optional.of(payment));
		when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
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
	}

	@Test
	@DisplayName("Refund keeps committed DB state and reports failure when PortOne cancel fails after DB commit")
	void refundPayment_portOneFailure_dbCommitted() {
		runTransactionsImmediately();
		Payment payment = confirmedPayment(10_000L, 2_000L, 8_000L);
		Order order = confirmedOrder(1L, orderItem(10L, 5_000L, 2L, 0L));
		when(paymentRepository.findByPaymentIdForUpdate("payment-123")).thenReturn(Optional.of(payment));
		when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
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
		assertThat(refundCaptor.getValue().getStatus()).isEqualTo(RefundStatus.COMPLETED);
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PARTIAL_REFUNDED);
	}

	private void runTransactionsImmediately() {
		when(transactionOperations.execute(any())).thenAnswer(invocation -> {
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

	private Order confirmedOrder(Long memberId, OrderItem... orderItems) {
		Member member = Member.create("user@test.com", "password", "user", "01000000000");
		ReflectionTestUtils.setField(member, "id", memberId);
		Order order = new Order(member, 10_000L, List.of(orderItems), 2_000L, "ORD-1");
		ReflectionTestUtils.setField(order, "id", 10L);
		order.markAsConfirmed();
		return order;
	}

	private OrderItem orderItem(Long id, Long price, Long quantity, Long stock) {
		Product product = Product.create(
			"product",
			price,
			stock,
			"description",
			ProductStatus.ON_SALE,
			ProductCategory.CLOTHING
		);
		ReflectionTestUtils.setField(product, "id", id);
		OrderItem orderItem = new OrderItem(product, price, quantity);
		ReflectionTestUtils.setField(orderItem, "id", id);
		return orderItem;
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
