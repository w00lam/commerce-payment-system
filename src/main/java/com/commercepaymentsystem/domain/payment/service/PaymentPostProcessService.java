package com.commercepaymentsystem.domain.payment.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commercepaymentsystem.domain.payment.entity.Payment;
import com.commercepaymentsystem.domain.payment.port.CartPort;
import com.commercepaymentsystem.domain.payment.port.OrderPort;
import com.commercepaymentsystem.domain.payment.port.PointPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentPostProcessService {

	private final OrderPort orderPort;
	private final PointPort pointPort;
	private final CartPort cartPort;

	public void process(Payment payment) {
		OrderPort.ConfirmedOrder confirmedOrder = orderPort.confirmOrder(
			payment.getOrderId(),
			payment.getMemberId()
		);

		if (payment.getUsedPointAmount() > 0) {
			pointPort.deductUsedPoint(
				payment.getMemberId(),
				payment.getUsedPointAmount(),
				payment.getId()
			);
		}

		if (payment.getEarnedPointAmount() > 0) {
			pointPort.earnPoint(
				payment.getMemberId(),
				payment.getEarnedPointAmount(),
				payment.getId()
			);
		}

		if (!confirmedOrder.cartItemIds().isEmpty()) {
			cartPort.deleteOrderedCartItems(
				payment.getMemberId(),
				confirmedOrder.cartItemIds()
			);
		}
	}
}
