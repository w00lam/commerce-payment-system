package com.commercepaymentsystem.domain.subscription.service;

import java.time.LocalDateTime;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.commercepaymentsystem.domain.point.entity.PointSourceType;
import com.commercepaymentsystem.domain.point.service.PointService;
import com.commercepaymentsystem.domain.subscription.entity.InvoiceStatus;
import com.commercepaymentsystem.domain.subscription.entity.Subscription;
import com.commercepaymentsystem.domain.subscription.entity.SubscriptionInvoice;
import com.commercepaymentsystem.domain.subscription.event.SubscriptionPaymentSucceededEvent;
import com.commercepaymentsystem.domain.subscription.exception.SubscriptionErrorCode;
import com.commercepaymentsystem.domain.subscription.exception.SubscriptionException;
import com.commercepaymentsystem.domain.subscription.repository.SubscriptionInvoiceRepository;
import com.commercepaymentsystem.domain.subscription.repository.SubscriptionRepository;

@Service
public class SubscriptionBillingFinalizer {

	private final SubscriptionRepository subscriptionRepository;
	private final SubscriptionInvoiceRepository subscriptionInvoiceRepository;
	private final PointService pointService;
	private final ApplicationEventPublisher eventPublisher;

	public SubscriptionBillingFinalizer(
		SubscriptionRepository subscriptionRepository,
		SubscriptionInvoiceRepository subscriptionInvoiceRepository,
		PointService pointService,
		ApplicationEventPublisher eventPublisher
	) {
		this.subscriptionRepository = subscriptionRepository;
		this.subscriptionInvoiceRepository = subscriptionInvoiceRepository;
		this.pointService = pointService;
		this.eventPublisher = eventPublisher;
	}

	/**
	 * 첫 구독 결제 결과를 독립 트랜잭션에서 반영합니다.
	 *
	 * <p>성공 시 인보이스/포인트/멤버십 이벤트를 처리하고, 실패 시 생성된 구독을 즉시 취소하여
	 * 외부 PG 호출 이후의 DB 상태를 일관되게 마무리합니다.</p>
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void finalizeFirstBilling(
		Long memberId,
		PreparedSubscriptionBilling billing,
		SubscriptionPaymentService.PaymentResult paymentResult
	) {
		Subscription subscription = getSubscription(billing.subscriptionId());
		SubscriptionInvoice invoice = getInvoice(billing.invoiceId());

		if (paymentResult.isSuccess()) {
			completeSuccessfulInvoice(memberId, invoice, paymentResult.getPortonePaymentId());
			return;
		}

		subscription.cancel();
		subscriptionRepository.save(subscription);
		invoice.markAsFailed(paymentResult.getPortonePaymentId(), paymentResult.getFailureReason());
		subscriptionInvoiceRepository.save(invoice);
	}

	/**
	 * 정기 청구 결제 결과를 독립 트랜잭션에서 반영합니다.
	 *
	 * <p>성공 시 포인트 적립과 다음 결제일 갱신을 수행하고, 실패 시 미납 상태와 실패 인보이스를
	 * 기록해 후속 재시도/정책 판단이 가능하게 합니다.</p>
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void finalizeScheduledBilling(
		PreparedSubscriptionBilling billing,
		SubscriptionPaymentService.PaymentResult paymentResult
	) {
		Subscription subscription = getSubscription(billing.subscriptionId());
		SubscriptionInvoice invoice = getInvoice(billing.invoiceId());

		if (paymentResult.isSuccess()) {
			completeSuccessfulInvoice(subscription.getMemberId(), invoice, paymentResult.getPortonePaymentId());

			boolean hasRemainingFailedInvoices = subscriptionInvoiceRepository.existsBySubscriptionIdAndStatus(
				subscription.getId(),
				InvoiceStatus.FAILED
			);
			if (!hasRemainingFailedInvoices) {
				subscription.clearUnpaid();
			}
			subscription.renewNextBillingDate();
			subscriptionRepository.save(subscription);
			return;
		}

		invoice.markAsFailed(paymentResult.getPortonePaymentId(), paymentResult.getFailureReason());
		subscriptionInvoiceRepository.save(invoice);
		subscription.markAsUnpaid();
		subscription.renewNextBillingDate();
		subscriptionRepository.save(subscription);
	}

	/**
	 * 실패 상태로 남아 있던 미납 인보이스의 재청구 성공/실패 결과를 반영합니다.
	 *
	 * <p>성공 시 최초 청구와 동일하게 포인트 적립과 멤버십 이벤트를 발행하되, 이미 정기 청구 실패
	 * 시점에 다음 결제일은 갱신됐으므로 재시도에서는 결제일을 다시 이동하지 않습니다.</p>
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void finalizeRetryBilling(
		PreparedSubscriptionBilling billing,
		SubscriptionPaymentService.PaymentResult paymentResult
	) {
		Subscription subscription = getSubscription(billing.subscriptionId());
		SubscriptionInvoice invoice = getInvoice(billing.invoiceId());

		if (!paymentResult.isSuccess()) {
			invoice.markAsFailed(paymentResult.getPortonePaymentId(), paymentResult.getFailureReason());
			subscriptionInvoiceRepository.save(invoice);
			return;
		}

		completeSuccessfulInvoice(subscription.getMemberId(), invoice, paymentResult.getPortonePaymentId());

		boolean hasRemainingFailedInvoices = subscriptionInvoiceRepository.existsBySubscriptionIdAndStatus(
			subscription.getId(),
			InvoiceStatus.FAILED
		);
		if (!hasRemainingFailedInvoices) {
			subscription.clearUnpaid();
			subscriptionRepository.save(subscription);
		}
	}

	private void completeSuccessfulInvoice(Long memberId, SubscriptionInvoice invoice, String portonePaymentId) {
		long earnedPoints = invoice.getBillingAmount() * invoice.getPointRewardRate() / 100;
		invoice.markAsSucceeded(portonePaymentId, earnedPoints, LocalDateTime.now());
		subscriptionInvoiceRepository.save(invoice);

		if (earnedPoints > 0) {
			pointService.earnPoint(memberId, earnedPoints, invoice.getId(), PointSourceType.SUBSCRIPTION);
		}
		eventPublisher.publishEvent(new SubscriptionPaymentSucceededEvent(
			memberId,
			invoice.getBillingAmount(),
			invoice.getId()
		));
	}

	private Subscription getSubscription(Long subscriptionId) {
		return subscriptionRepository.findById(subscriptionId)
			.orElseThrow(() -> new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));
	}

	private SubscriptionInvoice getInvoice(Long invoiceId) {
		return subscriptionInvoiceRepository.findById(invoiceId)
			.orElseThrow(() -> new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));
	}
}
