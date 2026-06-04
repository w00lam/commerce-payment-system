package com.commercepaymentsystem.domain.webhook.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.commercepaymentsystem.domain.payment.entity.Payment;
import com.commercepaymentsystem.domain.payment.service.PaymentPostProcessService;
import com.commercepaymentsystem.domain.payment.service.PaymentService;
import com.commercepaymentsystem.domain.payment.service.PaymentService.PaymentConfirmation;
import com.commercepaymentsystem.domain.refund.entity.Refund;
import com.commercepaymentsystem.domain.refund.service.RefundPostProcessService;
import com.commercepaymentsystem.domain.refund.service.RefundService;
import com.commercepaymentsystem.domain.webhook.dto.WebhookCommand;
import com.commercepaymentsystem.domain.webhook.entity.WebhookEvent;
import com.commercepaymentsystem.domain.webhook.entity.WebhookStatus;
import com.commercepaymentsystem.domain.webhook.exception.WebhookErrorCode;
import com.commercepaymentsystem.domain.webhook.exception.WebhookException;
import com.commercepaymentsystem.domain.webhook.repository.WebhookRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WebhookProcessor {

	private static final List<WebhookStatus> FINISHED_STATUSES = List.of(
		WebhookStatus.COMPLETED,
		WebhookStatus.IGNORED
	);

	private final WebhookRepository webhookRepository;
	private final PaymentService paymentService;
	private final PaymentPostProcessService paymentPostProcessService;
	private final RefundService refundService;
	private final RefundPostProcessService refundPostProcessService;

	/**
	 * 결제 또는 환불 부수 효과를 웹훅 수신 기록과 분리된 트랜잭션에서 실행합니다.
	 *
	 * <p>이 트랜잭션이 실패해도 웹훅 수신 기록은 커밋된 상태로 남고, 호출자가 별도
	 * 트랜잭션에서 FAILED 상태를 기록합니다. 이 경계 덕분에 외부 재전송 실패가
	 * 조용히 사라지지 않고 추적 가능한 상태로 남습니다.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public WebhookStatus process(String eventId, WebhookCommand command) {
		WebhookEvent event = webhookRepository.findById(eventId)
			.orElseThrow(() -> new WebhookException(WebhookErrorCode.INVALID_PAYLOAD));

		if (event.getStatus() == WebhookStatus.COMPLETED || event.getStatus() == WebhookStatus.IGNORED) {
			return event.getStatus();
		}

		/*
		 * 외부 PG는 의미상 같은 결제 이벤트를 다른 이벤트 ID로 재전송할 수 있습니다.
		 * 결제 단위 상태 확인으로 포인트 차감/적립, 재고 복구, 주문 상태 변경 같은 후처리가
		 * 반복 실행되지 않게 막습니다.
		 */
		if (isFinishedPaymentWebhook(command)) {
			event.ignore("Already processed payment webhook.");
			return event.getStatus();
		}

		switch (command.eventType()) {
			case PAYMENT_PAID -> processPaid(event, command.paymentId());
			case PAYMENT_CANCELLED, PAYMENT_PARTIAL_CANCELLED -> processRefund(event, command.paymentId());
			default -> event.ignore("Unsupported webhook event.");
		}

		return event.getStatus();
	}

	private void processPaid(WebhookEvent event, String paymentId) {
		PaymentConfirmation confirmation = paymentService.confirmPaymentFromWebhook(paymentId);
		if (!confirmation.confirmedNow()) {
			event.ignore("Payment is already confirmed.");
			return;
		}

		paymentPostProcessService.process(confirmation.payment());
		event.complete("Payment webhook processing completed.");
	}

	private void processRefund(WebhookEvent event, String paymentId) {
		Payment payment = paymentService.getPaymentByPaymentIdForUpdate(paymentId);
		Optional<Refund> processingRefund = refundService.findProcessingRefund(payment.getId());
		if (processingRefund.isEmpty()) {
			event.ignore("No processing refund exists.");
			return;
		}

		Refund refund = refundService.completeRefund(processingRefund.get().getId());
		boolean isFullRefund = refundService.isFullRefund(
			payment.getUsedPointAmount(),
			payment.getFinalPaymentAmount(),
			refundService.getExistingRefunds(payment.getId()),
			refund
		);
		refundPostProcessService.process(payment, payment.getOrderId(), refund, isFullRefund);
		paymentService.updateRefundStatus(payment, isFullRefund);
		event.complete("Refund webhook processing completed.");
	}

	private boolean isFinishedPaymentWebhook(WebhookCommand command) {
		return webhookRepository.existsByPaymentIdAndEventTypeAndStatusIn(
			command.paymentId(),
			command.eventType().name(),
			FINISHED_STATUSES
		);
	}
}
