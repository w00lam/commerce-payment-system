package com.commercepaymentsystem.domain.payment.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

import com.commercepaymentsystem.domain.payment.dto.PaymentCreateCommand;
import com.commercepaymentsystem.domain.payment.dto.PaymentCreateResult;
import com.commercepaymentsystem.domain.payment.entity.Payment;
import com.commercepaymentsystem.domain.payment.entity.PaymentStatus;
import com.commercepaymentsystem.domain.payment.exception.PaymentErrorCode;
import com.commercepaymentsystem.domain.payment.exception.PaymentException;
import com.commercepaymentsystem.domain.payment.repository.PaymentRepository;

class PaymentServiceTest {

	private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
	private final PaymentIdGenerator paymentIdGenerator = mock(PaymentIdGenerator.class);

	private final PaymentService paymentService = new PaymentService(
		paymentRepository,
		paymentIdGenerator
	);

	@Test
	@DisplayName("Payment creation succeeds with pending status")
	void createPendingPayment_success() {
		// given
		PaymentCreateCommand command = new PaymentCreateCommand(
			1L,
			10L,
			10_000L,
			2_000L,
			8_000L
		);

		when(paymentIdGenerator.generate()).thenReturn("PAY-1234");
		when(paymentRepository.existsByPaymentId("PAY-1234")).thenReturn(false);
		when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// when
		PaymentCreateResult result = paymentService.createPendingPayment(command);

		// then
		ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
		verify(paymentRepository).save(paymentCaptor.capture());

		Payment savedPayment = paymentCaptor.getValue();

		assertThat(savedPayment.getPaymentId()).isEqualTo("PAY-1234");
		assertThat(savedPayment.getMemberId()).isEqualTo(1L);
		assertThat(savedPayment.getOrderId()).isEqualTo(10L);
		assertThat(savedPayment.getTotalOrderAmount()).isEqualTo(10_000L);
		assertThat(savedPayment.getUsedPointAmount()).isEqualTo(2_000L);
		assertThat(savedPayment.getFinalPaymentAmount()).isEqualTo(8_000L);
		assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);

		assertThat(result.paymentId()).isEqualTo("PAY-1234");
		assertThat(result.status()).isEqualTo(PaymentStatus.PENDING);
	}

	@Test
	@DisplayName("Payment id generation retries when duplicated")
	void createPendingPayment_retryDuplicatedPaymentId() {
		// given
		PaymentCreateCommand command = new PaymentCreateCommand(
			1L,
			10L,
			10_000L,
			0L,
			10_000L
		);

		when(paymentIdGenerator.generate()).thenReturn(
			"PAY-duplicated",
			"PAY-unique"
		);
		when(paymentRepository.existsByPaymentId("PAY-duplicated")).thenReturn(true);
		when(paymentRepository.existsByPaymentId("PAY-unique")).thenReturn(false);
		when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// when
		PaymentCreateResult result = paymentService.createPendingPayment(command);

		// then
		assertThat(result.paymentId()).isEqualTo("PAY-unique");
		verify(paymentIdGenerator, times(2)).generate();
	}

	@Test
	@DisplayName("Invalid member id fails payment creation")
	void createPendingPayment_invalidMemberId_fail() {
		// given
		PaymentCreateCommand command = new PaymentCreateCommand(
			0L,
			10L,
			10_000L,
			0L,
			10_000L
		);

		// when & then
		assertPaymentException(
			() -> paymentService.createPendingPayment(command),
			PaymentErrorCode.INVALID_MEMBER_ID
		);
		verify(paymentRepository, never()).save(any(Payment.class));
	}

	@Test
	@DisplayName("Invalid order id fails payment creation")
	void createPendingPayment_invalidOrderId_fail() {
		// given
		PaymentCreateCommand command = new PaymentCreateCommand(
			1L,
			0L,
			10_000L,
			0L,
			10_000L
		);

		// when & then
		assertPaymentException(
			() -> paymentService.createPendingPayment(command),
			PaymentErrorCode.INVALID_ORDER_ID
		);
		verify(paymentRepository, never()).save(any(Payment.class));
	}

	@Test
	@DisplayName("Mismatched final amount fails payment creation")
	void createPendingPayment_mismatchedFinalAmount_fail() {
		// given
		PaymentCreateCommand command = new PaymentCreateCommand(
			1L,
			10L,
			10_000L,
			2_000L,
			9_000L
		);

		// when & then
		assertPaymentException(
			() -> paymentService.createPendingPayment(command),
			PaymentErrorCode.INVALID_AMOUNT
		);
		verify(paymentRepository, never()).save(any(Payment.class));
	}

	@Test
	@DisplayName("Point amount greater than total amount fails payment creation")
	void createPendingPayment_pointGreaterThanTotalAmount_fail() {
		// given
		PaymentCreateCommand command = new PaymentCreateCommand(
			1L,
			10L,
			10_000L,
			11_000L,
			0L
		);

		// when & then
		assertPaymentException(
			() -> paymentService.createPendingPayment(command),
			PaymentErrorCode.INVALID_AMOUNT
		);
		verify(paymentRepository, never()).save(any(Payment.class));
	}

	@Test
	@DisplayName("Payment id generation fails after max retry attempts")
	void createPendingPayment_paymentIdGenerationFailed_fail() {
		// given
		PaymentCreateCommand command = new PaymentCreateCommand(
			1L,
			10L,
			10_000L,
			0L,
			10_000L
		);

		when(paymentIdGenerator.generate()).thenReturn("PAY-duplicated");
		when(paymentRepository.existsByPaymentId("PAY-duplicated")).thenReturn(true);

		// when & then
		assertPaymentException(
			() -> paymentService.createPendingPayment(command),
			PaymentErrorCode.PAYMENT_ID_GENERATION_FAILED
		);
		verify(paymentIdGenerator, times(5)).generate();
		verify(paymentRepository, never()).save(any(Payment.class));
	}

	private void assertPaymentException(
		ThrowingCallable callable,
		PaymentErrorCode expectedErrorCode
	) {
		assertThatThrownBy(callable)
			.isInstanceOf(PaymentException.class)
			.extracting("errorCode")
			.isEqualTo(expectedErrorCode);
	}
}
