package com.commercepaymentsystem.domain.payment.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commercepaymentsystem.domain.payment.dto.PaymentCreateCommand;
import com.commercepaymentsystem.domain.payment.dto.PaymentCreateResult;
import com.commercepaymentsystem.domain.payment.entity.Payment;
import com.commercepaymentsystem.domain.payment.exception.PaymentErrorCode;
import com.commercepaymentsystem.domain.payment.exception.PaymentException;
import com.commercepaymentsystem.domain.payment.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

	private static final int PAYMENT_ID_GENERATION_MAX_ATTEMPTS = 5;

	private final PaymentRepository paymentRepository;
	private final PaymentIdGenerator paymentIdGenerator;

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
