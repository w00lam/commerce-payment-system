package com.commercepaymentsystem.domain.refund.service;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commercepaymentsystem.domain.payment.entity.Payment;
import com.commercepaymentsystem.domain.refund.entity.Refund;
import com.commercepaymentsystem.domain.refund.entity.RefundItem;
import com.commercepaymentsystem.domain.refund.port.RefundOrderPort;
import com.commercepaymentsystem.domain.refund.port.RefundPointPort;
import com.commercepaymentsystem.domain.refund.port.RefundProductPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class RefundPostProcessService {

	private final RefundOrderPort refundOrderPort;
	private final RefundPointPort refundPointPort;
	private final RefundProductPort refundProductPort;

	public void process(Payment payment, Long orderId, Refund refund, boolean isFullRefund) {
		Map<Long, Long> productQuantities = refundOrderPort.restoreProductStock(orderId, refundQuantities(refund));
		refundProductPort.restoreProductStocks(productQuantities);
		restorePoint(payment, refund);
		revokeEarnedPoint(payment, refund, isFullRefund);

		if (isFullRefund) {
			refundOrderPort.cancelOrder(orderId);
		}
	}

	private void restorePoint(Payment payment, Refund refund) {
		if (refund.getPointRefundAmount() <= 0) {
			return;
		}
		refundPointPort.restorePoint(
			payment.getMemberId(),
			refund.getPointRefundAmount(),
			payment.getId(),
			refund.getId()
		);
	}

	private void revokeEarnedPoint(Payment payment, Refund refund, boolean isFullRefund) {
		long revokeAmount = calculateEarnedPointRevokeAmount(payment, refund, isFullRefund);
		if (revokeAmount <= 0) {
			return;
		}

		refundPointPort.revokeEarnedPoint(
			payment.getMemberId(),
			revokeAmount,
			payment.getId(),
			refund.getId()
		);
	}

	private long calculateEarnedPointRevokeAmount(Payment payment, Refund refund, boolean isFullRefund) {
		if (payment.getEarnedPointAmount() <= 0 || payment.getFinalPaymentAmount() <= 0) {
			return 0L;
		}
		if (isFullRefund) {
			long alreadyRevokedAmount = refundPointPort.getRevokedEarnedPointAmount(payment.getId());
			return payment.getEarnedPointAmount() - alreadyRevokedAmount;
		}

		return refund.getPgRefundAmount() * payment.getEarnedPointAmount() / payment.getFinalPaymentAmount();
	}

	private Map<Long, Long> refundQuantities(Refund refund) {
		return refund.getItems().stream()
			.collect(Collectors.toMap(
				RefundItem::getOrderItemId,
				RefundItem::getRefundQuantity,
				Long::sum
			));
	}
}
