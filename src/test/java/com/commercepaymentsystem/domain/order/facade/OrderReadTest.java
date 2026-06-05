package com.commercepaymentsystem.domain.order.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.commercepaymentsystem.domain.payment.dto.PaymentCreateResult;
import com.commercepaymentsystem.domain.payment.entity.Payment;
import com.commercepaymentsystem.domain.payment.entity.PaymentStatus;
import com.commercepaymentsystem.domain.payment.repository.PaymentRepository;
import com.commercepaymentsystem.domain.order.service.OrderNumberGenerator;
import com.commercepaymentsystem.domain.payment.service.PaymentIdGenerator;
import com.commercepaymentsystem.domain.payment.service.PaymentService;
import com.commercepaymentsystem.infrastructure.portone.client.PortOneClient;

@ExtendWith(MockitoExtension.class)
class OrderReadTest {

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private PaymentIdGenerator paymentIdGenerator;

	@Mock
	private OrderNumberGenerator orderNumberGenerator;

	@Mock
	private PortOneClient portOneClient;

	@InjectMocks
	private PaymentService paymentService;

	@Test
	@DisplayName("주문 ID로 결제 정보를 조회하면 PaymentCreateResult를 반환한다.")
	void findPaymentByOrderId_Success() {
		// given
		Long orderId = 1L;
		Long memberId = 1L;
		Long totalOrderAmount = 10000L;
		Long usedPointAmount = 1000L;
		Long finalPaymentAmount = 9000L;
		String paymentId = "PAY-20260603-000001";

		Payment payment = Payment.create(
			paymentId,
			memberId,
			orderId,
			totalOrderAmount,
			usedPointAmount,
			finalPaymentAmount
		);

		when(paymentRepository.findByOrderId(orderId))
			.thenReturn(Optional.of(payment));

		// when
		Optional<PaymentCreateResult> result = paymentService.findPaymentByOrderId(orderId);

		// then
		assertThat(result).isPresent();

		PaymentCreateResult paymentResult = result.get();

		assertThat(paymentResult.paymentId()).isEqualTo(paymentId);
		assertThat(paymentResult.memberId()).isEqualTo(memberId);
		assertThat(paymentResult.orderId()).isEqualTo(orderId);
		assertThat(paymentResult.totalOrderAmount()).isEqualTo(totalOrderAmount);
		assertThat(paymentResult.usedPointAmount()).isEqualTo(usedPointAmount);
		assertThat(paymentResult.finalPaymentAmount()).isEqualTo(finalPaymentAmount);
		assertThat(paymentResult.status()).isEqualTo(PaymentStatus.PENDING);

		verify(paymentRepository).findByOrderId(orderId);
	}

	@Test
	@DisplayName("주문 ID에 해당하는 결제 정보가 없으면 빈 Optional을 반환한다.")
	void findPaymentByOrderId_NotFound_ReturnsEmptyOptional() {
		// given
		Long orderId = 999L;

		when(paymentRepository.findByOrderId(orderId))
			.thenReturn(Optional.empty());

		// when
		Optional<PaymentCreateResult> result = paymentService.findPaymentByOrderId(orderId);

		// then
		assertThat(result).isEmpty();

		verify(paymentRepository).findByOrderId(orderId);
	}
}
