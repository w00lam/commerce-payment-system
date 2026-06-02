package com.commercepaymentsystem.domain.payment.service;

import java.time.Instant;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commercepaymentsystem.domain.payment.dto.PaymentConfirmCommand;
import com.commercepaymentsystem.domain.payment.dto.PaymentConfirmResult;
import com.commercepaymentsystem.domain.payment.dto.PaymentCreateCommand;
import com.commercepaymentsystem.domain.payment.dto.PaymentCreateResult;
import com.commercepaymentsystem.domain.payment.entity.Payment;
import com.commercepaymentsystem.domain.payment.exception.PaymentErrorCode;
import com.commercepaymentsystem.domain.payment.exception.PaymentException;
import com.commercepaymentsystem.domain.payment.repository.PaymentRepository;
import com.commercepaymentsystem.infrastructure.portone.PortOneClient;
import com.commercepaymentsystem.infrastructure.portone.dto.PortOnePaymentResponse;
import com.commercepaymentsystem.infrastructure.portone.exception.PortOnePaymentVerificationException;
import com.commercepaymentsystem.infrastructure.portone.exception.PortOneRetryableException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

	private static final int PAYMENT_ID_GENERATION_MAX_ATTEMPTS = 5;
	private static final String PAID_STATUS = "PAID";
	private static final String ORDER_NAME_PREFIX = "order-";

	private final PaymentRepository paymentRepository;
	private final PaymentIdGenerator paymentIdGenerator;
	private final PortOneClient portOneClient;

	/**
	 * 주문 생성 흐름에서 전달받은 결제 정보를 검증하고 결제 대기 상태의 Payment를 생성합니다.
	 *
	 * 주문 도메인이 회원, 주문, 상품 금액 정보를 확정한 뒤 이 메서드를 호출하는 것을 전제로 합니다.
	 * 결제 금액은 총 주문 금액에서 사용 포인트를 차감한 값과 최종 결제 금액이 일치해야 합니다.
	 *
	 * @param command 주문 생성 흐름에서 전달한 결제 생성 정보
	 * @return 저장된 결제 생성 결과
	 * @throws PaymentException 회원/주문 식별자 또는 결제 금액이 유효하지 않은 경우
	 */
	@Transactional
	public PaymentCreateResult createPendingPayment(PaymentCreateCommand command) {
		validateCommand(command);

		String paymentId = generateUniquePaymentId();
		Payment payment = Payment.create(
			paymentId,
			command.memberId(),
			command.orderId(),
			command.totalOrderAmount(),
			command.usedPointAmount(),
			command.finalPaymentAmount()
		);

		Payment savedPayment = paymentRepository.save(payment);

		return PaymentCreateResult.from(savedPayment);
	}

	/**
	 * 결제 확정 요청을 검증하고 Payment 상태를 확정 상태로 변경합니다.
	 *
	 * 같은 결제에 대한 중복 요청을 직렬화하기 위해 Payment를 쓰기 락으로 조회합니다.
	 * 이미 확정된 결제는 멱등 요청으로 보고 PortOne을 다시 호출하지 않은 채 기존 확정 결과를 반환합니다.
	 * 확정 가능한 상태라면 PortOne에서 실제 결제 정보를 조회해 결제 식별자, 주문 식별자, 결제 상태, 금액을 내부 Payment와 대조합니다.
	 *
	 * @param command 결제 확정에 필요한 결제 식별자와 인증 회원 식별자
	 * @return 확정된 결제 결과
	 * @throws PaymentException 결제가 없거나 소유권, 상태, PortOne 검증이 실패한 경우
	 */
	@Transactional
	public PaymentConfirmResult confirmPayment(PaymentConfirmCommand command) {
		validateConfirmCommand(command);

		Payment payment = loadPaymentForConfirm(command.paymentId());
		validateOwner(payment, command.memberId());

		if (payment.isConfirmed()) {
			return PaymentConfirmResult.from(payment);
		}

		validateConfirmableStatus(payment);

		PortOnePaymentResponse portOnePayment = loadPortOnePayment(command.paymentId());
		validatePortOnePayment(payment, portOnePayment);

		payment.confirm(resolvePaidAt(portOnePayment));

		return PaymentConfirmResult.from(payment);
	}

	/**
	 * 결제 생성에 필요한 식별자와 금액 입력값을 검증합니다.
	 *
	 * Order, Member 도메인의 실제 존재 여부는 각 도메인의 구현이 확정된 뒤 연동 지점에서 보강합니다.
	 */
	private void validateCommand(PaymentCreateCommand command) {
		if (command == null) {
			throw new PaymentException(PaymentErrorCode.INVALID_AMOUNT);
		}

		validateMemberId(command.memberId());
		validateOrderId(command.orderId());
		validateAmount(
			command.totalOrderAmount(),
			command.usedPointAmount(),
			command.finalPaymentAmount()
		);
	}

	/**
	 * 결제 확정에 필요한 최소 입력값을 검증합니다.
	 *
	 * 회원 식별자는 클라이언트가 전달한 값이 아니라 JWT 인증 결과에서 가져온 값이어야 합니다.
	 */
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

	/**
	 * 결제 금액 정책을 검증합니다.
	 *
	 * 음수 금액을 허용하지 않고, 사용 포인트는 총 주문 금액을 초과할 수 없습니다.
	 * 최종 결제 금액은 총 주문 금액에서 사용 포인트를 차감한 값과 정확히 일치해야 합니다.
	 */
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
	 * 결제 확정 대상 Payment를 쓰기 락으로 조회합니다.
	 *
	 * 확정 API와 웹훅이 동시에 같은 결제를 처리할 수 있으므로, 상태 검증부터 상태 변경까지 하나의 트랜잭션 안에서 직렬화합니다.
	 */
	private Payment loadPaymentForConfirm(String paymentId) {
		return paymentRepository.findByPaymentIdForUpdate(paymentId)
			.orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));
	}

	/**
	 * 결제 소유권을 검증합니다.
	 *
	 * Payment를 생성한 회원과 JWT로 인증된 회원이 같아야 결제를 확정할 수 있습니다.
	 */
	private void validateOwner(Payment payment, Long memberId) {
		if (!Objects.equals(payment.getMemberId(), memberId)) {
			throw new PaymentException(PaymentErrorCode.PAYMENT_OWNER_MISMATCH);
		}
	}

	/**
	 * 현재 Payment 상태가 확정 가능한 상태인지 검증합니다.
	 *
	 * 확정 가능한 상태 판단은 {@link com.commercepaymentsystem.domain.payment.entity.PaymentStatus}에 위임합니다.
	 */
	private void validateConfirmableStatus(Payment payment) {
		if (!payment.isConfirmable()) {
			throw new PaymentException(PaymentErrorCode.INVALID_PAYMENT_STATUS);
		}
	}

	/**
	 * PortOne에서 실제 결제 정보를 조회합니다.
	 *
	 * 클라이언트가 전달한 결제 결과는 신뢰하지 않고, 서버가 PortOne API를 직접 호출해 검증 기준 데이터를 가져옵니다.
	 * PortOne의 일시적 장애와 결제 검증 실패는 서로 다른 결제 예외로 변환합니다.
	 */
	private PortOnePaymentResponse loadPortOnePayment(String paymentId) {
		try {
			return portOneClient.getPayment(paymentId);
		} catch (PortOnePaymentVerificationException exception) {
			throw new PaymentException(PaymentErrorCode.PORTONE_PAYMENT_VERIFICATION_FAILED, exception.getMessage());
		} catch (PortOneRetryableException exception) {
			throw new PaymentException(PaymentErrorCode.PORTONE_PAYMENT_REQUEST_FAILED, exception.getMessage());
		}
	}

	/**
	 * PortOne 결제 정보와 내부 Payment 정보가 같은 결제를 가리키는지 검증합니다.
	 *
	 * PortOne 결제 식별자, 결제 상태, 주문 식별자, 최종 결제 금액이 모두 일치해야 결제를 확정할 수 있습니다.
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

	/**
	 * 내부 주문 식별자로 PortOne 주문 식별자 형식을 만듭니다.
	 *
	 * 결제 생성 시 PortOne에 전달한 주문 식별자 정책과 같은 규칙을 사용해야 합니다.
	 */
	private String expectedOrderName(Payment payment) {
		return ORDER_NAME_PREFIX + payment.getOrderId();
	}

	/**
	 * 결제 확정 시각을 결정합니다.
	 *
	 * PortOne 승인 시각이 있으면 그 값을 우선 사용하고, 없으면 서버 처리 시각을 사용합니다.
	 */
	private Instant resolvePaidAt(PortOnePaymentResponse portOnePayment) {
		if (portOnePayment.paidAt() != null) {
			return portOnePayment.paidAt();
		}

		return Instant.now();
	}

	/**
	 * PortOne 연동에 사용할 결제 식별자를 생성합니다.
	 *
	 * UUID 기반이라 충돌 가능성은 낮지만, 저장 전 중복 여부를 확인하고 제한 횟수 안에서 재시도합니다.
	 */
	private String generateUniquePaymentId() {
		for (int attempt = 0; attempt < PAYMENT_ID_GENERATION_MAX_ATTEMPTS; attempt++) {
			String paymentId = paymentIdGenerator.generate();

			if (!paymentRepository.existsByPaymentId(paymentId)) {
				return paymentId;
			}
		}

		throw new PaymentException(PaymentErrorCode.PAYMENT_ID_GENERATION_FAILED);
	}
}
