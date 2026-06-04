package com.commercepaymentsystem.domain.payment.service;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commercepaymentsystem.domain.payment.dto.PaymentConfirmCommand;
import com.commercepaymentsystem.domain.payment.dto.PaymentCreateCommand;
import com.commercepaymentsystem.domain.payment.dto.PaymentCreateResult;
import com.commercepaymentsystem.domain.payment.entity.Payment;
import com.commercepaymentsystem.domain.payment.exception.PaymentErrorCode;
import com.commercepaymentsystem.domain.payment.exception.PaymentException;
import com.commercepaymentsystem.domain.payment.repository.PaymentRepository;
import com.commercepaymentsystem.infrastructure.portone.client.PortOneClient;
import com.commercepaymentsystem.infrastructure.portone.dto.PortOnePaymentResponse;
import com.commercepaymentsystem.infrastructure.portone.exception.PortOneException;
import com.commercepaymentsystem.infrastructure.portone.exception.PortOneRetryableException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

	private static final int PAYMENT_ID_GENERATION_MAX_ATTEMPTS = 5;
	private static final String PAID_STATUS = "PAID";

	private final PaymentRepository paymentRepository;
	private final PaymentIdGenerator paymentIdGenerator;
	private final PortOneClient portOneClient;

	/**
	 * 주문에서 확정된 금액 정보를 기준으로 대기 상태 결제를 생성합니다.
	 */
	@Transactional
	public PaymentCreateResult createPendingPayment(PaymentCreateCommand command) {
		validateCommand(command);

		String paymentId = generateUniquePaymentId();
		Payment payment = Payment.create(
			paymentId,
			command.memberId(),
			command.orderId(),
			command.orderName(),
			command.totalOrderAmount(),
			command.usedPointAmount(),
			command.finalPaymentAmount()
		);

		Payment savedPayment = paymentRepository.save(payment);

		return PaymentCreateResult.from(savedPayment);
	}

	/**
	 * 결제 확정 요청을 검증하고 결제 상태를 확정으로 변경합니다.
	 */
	@Transactional
	public Payment confirmPayment(PaymentConfirmCommand command) {
		validateConfirmCommand(command);

		Payment payment = loadPaymentForConfirm(command.paymentId());
		validateOwner(payment, command.memberId());

		if (payment.isConfirmed()) {
			return payment;
		}

		validateConfirmableStatus(payment);

		if (isPointOnlyPayment(payment)) {
			payment.confirm(Instant.now());
			return payment;
		}

		PortOnePaymentResponse portOnePayment = loadPortOnePayment(command.paymentId());
		validatePortOnePayment(payment, portOnePayment);

		payment.confirm(resolvePaidAt(portOnePayment));

		return payment;
	}

	private void validateCommand(PaymentCreateCommand command) {
		if (command == null) {
			throw new PaymentException(PaymentErrorCode.INVALID_AMOUNT);
		}

		validateMemberId(command.memberId());
		validateOrderId(command.orderId());
		validateOrderName(command.orderName());
		validateAmount(
			command.totalOrderAmount(),
			command.usedPointAmount(),
			command.finalPaymentAmount()
		);
	}

	private void validateConfirmCommand(PaymentConfirmCommand command) {
		if (command == null) {
			throw new PaymentException(PaymentErrorCode.INVALID_PAYMENT_ID);
		}

		if (command.paymentId() == null || command.paymentId().isBlank()) {
			throw new PaymentException(PaymentErrorCode.INVALID_PAYMENT_ID);
		}

		validateMemberId(command.memberId());
	}

	private void validateMemberId(Long memberId) {
		if (memberId == null || memberId <= 0) {
			throw new PaymentException(PaymentErrorCode.INVALID_MEMBER_ID);
		}
	}

	private void validateOrderId(Long orderId) {
		if (orderId == null || orderId <= 0) {
			throw new PaymentException(PaymentErrorCode.INVALID_ORDER_ID);
		}
	}

	private void validateOrderName(String orderName) {
		if (orderName == null || orderName.isBlank()) {
			throw new PaymentException(PaymentErrorCode.INVALID_ORDER_ID);
		}
	}

	private void validateAmount(
		Long totalOrderAmount,
		Long usedPointAmount,
		Long finalPaymentAmount
	) {
		if (isNegativeOrNull(totalOrderAmount)
			|| isNegativeOrNull(usedPointAmount)
			|| isNegativeOrNull(finalPaymentAmount)
		) {
			throw new PaymentException(PaymentErrorCode.INVALID_AMOUNT);
		}

		if (usedPointAmount > totalOrderAmount) {
			throw new PaymentException(PaymentErrorCode.INVALID_AMOUNT);
		}

		long calculatedFinalPaymentAmount = totalOrderAmount - usedPointAmount;

		if (calculatedFinalPaymentAmount != finalPaymentAmount) {
			throw new PaymentException(PaymentErrorCode.INVALID_AMOUNT);
		}
	}

	private boolean isNegativeOrNull(Long amount) {
		return amount == null || amount < 0;
	}

	/**
	 * 동시에 같은 결제를 확정하지 못하도록 비관적 락으로 결제를 조회합니다.
	 */
	private Payment loadPaymentForConfirm(String paymentId) {
		return paymentRepository.findByPaymentIdForUpdate(paymentId)
			.orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));
	}

	private void validateOwner(Payment payment, Long memberId) {
		if (!Objects.equals(payment.getMemberId(), memberId)) {
			throw new PaymentException(PaymentErrorCode.PAYMENT_OWNER_MISMATCH);
		}
	}

	private void validateConfirmableStatus(Payment payment) {
		if (!payment.isConfirmable()) {
			throw new PaymentException(PaymentErrorCode.INVALID_PAYMENT_STATUS);
		}
	}

	private boolean isPointOnlyPayment(Payment payment) {
		return payment.getFinalPaymentAmount() == 0;
	}

	private PortOnePaymentResponse loadPortOnePayment(String paymentId) {
		try {
			return portOneClient.getPayment(paymentId);
		} catch (PortOneRetryableException exception) {
			throw new PaymentException(PaymentErrorCode.PORTONE_PAYMENT_REQUEST_FAILED, exception);
		} catch (PortOneException exception) {
			throw new PaymentException(PaymentErrorCode.PORTONE_PAYMENT_VERIFICATION_FAILED, exception);
		}
	}

	/**
	 * 포트원 결제 정보가 서버에 저장된 결제와 같은 결제인지 검증합니다.
	 */
	private void validatePortOnePayment(Payment payment, PortOnePaymentResponse portOnePayment) {
		if (portOnePayment == null
			|| !Objects.equals(portOnePayment.id(), payment.getPaymentId())
			|| !PAID_STATUS.equals(portOnePayment.status())
			|| !Objects.equals(portOnePayment.orderName(), expectedOrderName(payment))
			|| !Objects.equals(portOnePayment.totalAmount(), payment.getFinalPaymentAmount())
		) {
			throw new PaymentException(PaymentErrorCode.PORTONE_PAYMENT_VERIFICATION_FAILED);
		}
	}

	private String expectedOrderName(Payment payment) {
		if (payment.getOrderName() == null || payment.getOrderName().isBlank()) {
			return "order-" + payment.getOrderId();
		}

		return payment.getOrderName();
	}

	private Instant resolvePaidAt(PortOnePaymentResponse portOnePayment) {
		if (portOnePayment.paidAt() != null) {
			return portOnePayment.paidAt();
		}

		return Instant.now();
	}

	private String generateUniquePaymentId() {
		for (int attempt = 0; attempt < PAYMENT_ID_GENERATION_MAX_ATTEMPTS; attempt++) {
			String paymentId = paymentIdGenerator.generate();

			if (!paymentRepository.existsByPaymentId(paymentId)) {
				return paymentId;
			}
		}

		throw new PaymentException(PaymentErrorCode.PAYMENT_ID_GENERATION_FAILED);
	}

	@Transactional
	public Payment loadAndValidatePaymentForRefund(String paymentId, Long memberId) {
		Payment payment = paymentRepository.findByPaymentIdForUpdate(paymentId)
			.orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));

		if (!Objects.equals(payment.getMemberId(), memberId)) {
			throw new PaymentException(PaymentErrorCode.PAYMENT_OWNER_MISMATCH);
		}

		if (!payment.isRefundable()) {
			throw new PaymentException(PaymentErrorCode.INVALID_PAYMENT_STATUS);
		}

		return payment;
	}

	@Transactional
	public PaymentConfirmation confirmPaymentFromWebhook(String paymentId) {
		if (paymentId == null || paymentId.isBlank()) {
			throw new PaymentException(PaymentErrorCode.INVALID_PAYMENT_ID);
		}

		Payment payment = loadPaymentForConfirm(paymentId);
		if (payment.isConfirmed()) {
			return new PaymentConfirmation(payment, false);
		}

		validateConfirmableStatus(payment);

		PortOnePaymentResponse portOnePayment = loadPortOnePayment(paymentId);
		validatePortOnePayment(payment, portOnePayment);

		payment.confirm(resolvePaidAt(portOnePayment));

		return new PaymentConfirmation(payment, true);
	}

	@Transactional
	public Payment getPaymentByPaymentIdForUpdate(String paymentId) {
		if (paymentId == null || paymentId.isBlank()) {
			throw new PaymentException(PaymentErrorCode.INVALID_PAYMENT_ID);
		}

		return paymentRepository.findByPaymentIdForUpdate(paymentId)
			.orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));
	}

	public Payment getPaymentById(Long id) {
		return paymentRepository.findById(id)
			.orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));
	}

	@Transactional
	public void updateRefundStatus(Payment payment, boolean isFullRefund) {
		if (isFullRefund) {
			payment.markRefunded();
		} else {
			payment.markPartiallyRefunded();
		}
	}

	@Transactional
	public Payment getPendingPaymentByOrderIdForUpdate(Long orderId, Long memberId) {
		validateOrderId(orderId);
		validateMemberId(memberId);

		Payment payment = paymentRepository.findByOrderIdForUpdate(orderId)
			.orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));

		validateOwner(payment, memberId);

		if (!payment.isPending()) {
			throw new PaymentException(PaymentErrorCode.INVALID_PAYMENT_STATUS);
		}

		return payment;
	}

	@Transactional
	public void failPayment(Payment payment) {
		if (!payment.isPending()) {
			throw new PaymentException(PaymentErrorCode.INVALID_PAYMENT_STATUS);
		}

		payment.fail();
	}

	@Transactional(readOnly = true)
	public Optional<PaymentCreateResult> findPaymentByOrderId(Long orderId) {
		return paymentRepository.findByOrderId(orderId).map(PaymentCreateResult::from);
	}

	public record PaymentConfirmation(
		Payment payment,
		boolean confirmedNow
	) {
	}
}
