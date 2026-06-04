package com.commercepaymentsystem.domain.refund.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commercepaymentsystem.domain.payment.entity.Payment;
import com.commercepaymentsystem.domain.refund.dto.RefundCommand;
import com.commercepaymentsystem.domain.refund.dto.RefundItemCommand;
import com.commercepaymentsystem.domain.refund.entity.Refund;
import com.commercepaymentsystem.domain.refund.entity.RefundItem;
import com.commercepaymentsystem.domain.refund.entity.RefundStatus;
import com.commercepaymentsystem.domain.refund.exception.RefundErrorCode;
import com.commercepaymentsystem.domain.refund.exception.RefundException;
import com.commercepaymentsystem.domain.refund.port.RefundOrderPort.RefundableOrderInfo;
import com.commercepaymentsystem.domain.refund.port.RefundOrderPort.RefundableOrderInfo.RefundableOrderItemInfo;
import com.commercepaymentsystem.domain.refund.repository.RefundRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefundService {

	private final RefundRepository refundRepository;

	public void validateCommand(RefundCommand command) {
		if (command == null || command.paymentId() == null || command.paymentId().isBlank()) {
			throw new RefundException(RefundErrorCode.INVALID_PAYMENT_ID);
		}
		if (command.memberId() == null || command.memberId() <= 0) {
			throw new RefundException(RefundErrorCode.PAYMENT_OWNER_MISMATCH);
		}
		if (command.reason() == null || command.reason().isBlank()) {
			throw new RefundException(RefundErrorCode.INVALID_REASON);
		}
		if (command.items() == null || command.items().isEmpty()) {
			throw new RefundException(RefundErrorCode.INVALID_REFUND_ITEM);
		}
	}

	@Transactional
	public PreparedRefund prepareRefund(RefundCommand command, Payment payment, RefundableOrderInfo order) {
		Map<Long, RefundableOrderItemInfo> orderItems = order.orderItems().stream()
			.collect(Collectors.toMap(RefundableOrderItemInfo::orderItemId, item -> item));
		Map<Long, Long> requestedQuantities = mergeRequestedQuantities(command.items());
		List<Refund> existingRefunds = refundRepository.findByPaymentId(payment.getId());
		Map<Long, Long> refundedQuantities = refundedQuantities(existingRefunds);
		RefundAmounts alreadyRefunded = refundedAmounts(existingRefunds);
		List<PreparedRefundItem> preparedItems = prepareItems(
			requestedQuantities,
			orderItems,
			refundedQuantities,
			payment,
			alreadyRefunded
		);

		RefundAmounts refundAmounts = sumAmounts(preparedItems);
		Refund refund = Refund.create(
			payment.getId(),
			command.reason(),
			refundAmounts.pointAmount(),
			refundAmounts.pgAmount()
		);
		preparedItems.forEach(item -> refund.addItem(RefundItem.create(
			item.orderItemId(),
			item.quantity(),
			item.pointAmount(),
			item.pgAmount()
		)));
		refund.startProcessing();

		Refund savedRefund = refundRepository.save(refund);
		long currentPgCancellableAmount = payment.getFinalPaymentAmount() - alreadyRefunded.pgAmount();

		return new PreparedRefund(
			savedRefund.getId(),
			payment.getPaymentId(),
			refundAmounts.pgAmount(),
			currentPgCancellableAmount,
			command.reason()
		);
	}

	@Transactional
	public Refund completeRefund(Long refundId) {
		Refund refund = refundRepository.findById(refundId)
			.orElseThrow(() -> new RefundException(RefundErrorCode.INVALID_PAYMENT_ID));
		refund.complete();
		return refund;
	}

	@Transactional
	public Refund failRefund(Long refundId) {
		Refund refund = refundRepository.findById(refundId)
			.orElseThrow(() -> new RefundException(RefundErrorCode.INVALID_PAYMENT_ID));
		refund.fail();
		return refund;
	}

	@Transactional
	public Refund failPostProcess(Long refundId) {
		Refund refund = refundRepository.findById(refundId)
			.orElseThrow(() -> new RefundException(RefundErrorCode.INVALID_PAYMENT_ID));
		refund.failPostProcess();
		return refund;
	}

	public List<Refund> getExistingRefunds(Long paymentId) {
		return refundRepository.findByPaymentId(paymentId);
	}

	public boolean isFullRefund(
		Long usedPointAmount,
		Long finalPaymentAmount,
		List<Refund> refunds,
		Refund currentRefund
	) {
		RefundAmounts refundedAmounts = refundedAmounts(refunds);
		if (refunds.stream().noneMatch(r -> Objects.equals(r.getId(), currentRefund.getId()))) {
			refundedAmounts = new RefundAmounts(
				refundedAmounts.pointAmount() + currentRefund.getPointRefundAmount(),
				refundedAmounts.pgAmount() + currentRefund.getPgRefundAmount()
			);
		}
		return Objects.equals(refundedAmounts.pointAmount(), usedPointAmount) &&
			Objects.equals(refundedAmounts.pgAmount(), finalPaymentAmount);
	}

	public RefundAmounts getRefundedAmounts(List<Refund> refunds) {
		return refundedAmounts(refunds);
	}

	private Map<Long, Long> mergeRequestedQuantities(List<RefundItemCommand> items) {
		Map<Long, Long> requestedQuantities = new HashMap<>();
		for (RefundItemCommand item : items) {
			if (
				item == null ||
				item.orderItemId() == null ||
				item.orderItemId() <= 0 ||
				item.quantity() == null ||
				item.quantity() <= 0
			) {
				throw new RefundException(RefundErrorCode.INVALID_REFUND_ITEM);
			}
			requestedQuantities.merge(item.orderItemId(), item.quantity(), Long::sum);
		}
		return requestedQuantities;
	}

	private List<PreparedRefundItem> prepareItems(
		Map<Long, Long> requestedQuantities,
		Map<Long, RefundableOrderItemInfo> orderItems,
		Map<Long, Long> refundedQuantities,
		Payment payment,
		RefundAmounts alreadyRefunded
	) {
		long requestedTotalAmount = 0;
		for (Map.Entry<Long, Long> entry : requestedQuantities.entrySet()) {
			RefundableOrderItemInfo orderItem = orderItems.get(entry.getKey());
			if (orderItem == null) {
				throw new RefundException(RefundErrorCode.INVALID_REFUND_ITEM);
			}

			long alreadyRefundedQuantity = refundedQuantities.getOrDefault(entry.getKey(), 0L);
			long remainingQuantity = orderItem.quantity() - alreadyRefundedQuantity;
			if (entry.getValue() > remainingQuantity) {
				throw new RefundException(RefundErrorCode.REFUND_AMOUNT_EXCEEDED);
			}

			requestedTotalAmount += orderItem.orderPrice() * entry.getValue();
		}

		RefundAmounts totalAmounts = splitTotalRefundAmount(
			requestedTotalAmount,
			isLastRefund(requestedQuantities, orderItems, refundedQuantities),
			payment,
			alreadyRefunded
		);

		long remainingPointAmount = totalAmounts.pointAmount();
		long remainingPgAmount = totalAmounts.pgAmount();
		List<Map.Entry<Long, Long>> requestedEntries = List.copyOf(requestedQuantities.entrySet());
		List<PreparedRefundItem> preparedItems = new java.util.ArrayList<>();

		for (int i = 0; i < requestedEntries.size(); i++) {
			Map.Entry<Long, Long> entry = requestedEntries.get(i);
			RefundableOrderItemInfo orderItem = orderItems.get(entry.getKey());
			long itemTotalAmount = orderItem.orderPrice() * entry.getValue();
			long itemPointAmount;
			long itemPgAmount;
			if (i == requestedEntries.size() - 1) {
				itemPointAmount = remainingPointAmount;
				itemPgAmount = remainingPgAmount;
			} else {
				itemPointAmount = itemTotalAmount * totalAmounts.pointAmount() / requestedTotalAmount;
				itemPgAmount = itemTotalAmount - itemPointAmount;
				remainingPointAmount -= itemPointAmount;
				remainingPgAmount -= itemPgAmount;
			}

			preparedItems.add(new PreparedRefundItem(
				entry.getKey(),
				entry.getValue(),
				itemPointAmount,
				itemPgAmount
			));
		}

		return preparedItems;
	}

	private RefundAmounts splitTotalRefundAmount(
		long requestedTotalAmount,
		boolean isLastRefund,
		Payment payment,
		RefundAmounts alreadyRefunded
	) {
		long remainingPointAmount = payment.getUsedPointAmount() - alreadyRefunded.pointAmount();
		long remainingPgAmount = payment.getFinalPaymentAmount() - alreadyRefunded.pgAmount();
		if (requestedTotalAmount > remainingPointAmount + remainingPgAmount) {
			throw new RefundException(RefundErrorCode.REFUND_AMOUNT_EXCEEDED);
		}
		if (isLastRefund) {
			return new RefundAmounts(remainingPointAmount, remainingPgAmount);
		}
		if (payment.getTotalOrderAmount() == null || payment.getTotalOrderAmount() <= 0) {
			throw new RefundException(RefundErrorCode.INVALID_AMOUNT);
		}

		long pointAmount = requestedTotalAmount * payment.getUsedPointAmount() / payment.getTotalOrderAmount();
		long pgAmount = requestedTotalAmount - pointAmount;
		if (pointAmount > remainingPointAmount || pgAmount > remainingPgAmount) {
			throw new RefundException(RefundErrorCode.REFUND_AMOUNT_EXCEEDED);
		}
		return new RefundAmounts(pointAmount, pgAmount);
	}

	private boolean isLastRefund(
		Map<Long, Long> requestedQuantities,
		Map<Long, RefundableOrderItemInfo> orderItems,
		Map<Long, Long> refundedQuantities
	) {
		for (RefundableOrderItemInfo orderItem : orderItems.values()) {
			long requestedQuantity = requestedQuantities.getOrDefault(orderItem.orderItemId(), 0L);
			long alreadyRefundedQuantity = refundedQuantities.getOrDefault(orderItem.orderItemId(), 0L);
			if (alreadyRefundedQuantity + requestedQuantity < orderItem.quantity()) {
				return false;
			}
		}
		return true;
	}

	private Map<Long, Long> refundedQuantities(List<Refund> refunds) {
		Map<Long, Long> refundedQuantities = new HashMap<>();
		for (Refund refund : refunds) {
			if (!isRefundedOrPgCancelled(refund)) {
				continue;
			}
			for (RefundItem item : refund.getItems()) {
				refundedQuantities.merge(item.getOrderItemId(), item.getRefundQuantity(), Long::sum);
			}
		}
		return refundedQuantities;
	}

	private RefundAmounts refundedAmounts(List<Refund> refunds) {
		long pointAmount = 0;
		long pgAmount = 0;
		for (Refund refund : refunds) {
			if (!isRefundedOrPgCancelled(refund)) {
				continue;
			}
			pointAmount += refund.getPointRefundAmount();
			pgAmount += refund.getPgRefundAmount();
		}
		return new RefundAmounts(pointAmount, pgAmount);
	}

	private boolean isRefundedOrPgCancelled(Refund refund) {
		// PROCESSING is only an in-flight reservation. Count only refunds that are completed
		// or whose PG cancellation has already succeeded.
		return refund.getStatus() == RefundStatus.COMPLETED
			|| refund.getStatus() == RefundStatus.POST_PROCESS_FAILED;
	}

	private RefundAmounts sumAmounts(List<PreparedRefundItem> items) {
		long pointAmount = items.stream()
			.mapToLong(PreparedRefundItem::pointAmount)
			.sum();
		long pgAmount = items.stream()
			.mapToLong(PreparedRefundItem::pgAmount)
			.sum();
		return new RefundAmounts(pointAmount, pgAmount);
	}

	public record PreparedRefund(
		Long refundId,
		String portOnePaymentId,
		Long pgAmount,
		Long currentPgCancellableAmount,
		String reason
	) {
	}

	private record PreparedRefundItem(
		Long orderItemId,
		Long quantity,
		Long pointAmount,
		Long pgAmount
	) {
	}

	public record RefundAmounts(
		Long pointAmount,
		Long pgAmount
	) {
	}
}
