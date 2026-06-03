package com.commercepaymentsystem.domain.order.entity;

import java.util.ArrayList;
import java.util.List;

import com.commercepaymentsystem.domain.member.entity.Member;
import com.commercepaymentsystem.domain.order.exception.OrderErrorCode;
import com.commercepaymentsystem.global.entity.BaseEntity;
import com.commercepaymentsystem.global.exception.BusinessException;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@Column(nullable = false, unique = true, length = 50)
	private String orderNumber;

	@Column(name = "total_price", nullable = false, columnDefinition = "int UNSIGNED")
	private Long totalPrice;

	@Column(nullable = false, columnDefinition = "INT UNSIGNED")
	private Long usedPointAmount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private OrderStatus status = OrderStatus.CREATED;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<OrderItem> orderItems = new ArrayList<>();

	public Order(Member member, Long totalPrice, List<OrderItem> orderItems) {
		this.member = member;
		this.totalPrice = totalPrice;
		orderItems.forEach(this::addOrderItem);
	}

	public Order(Member member, Long totalPrice, List<OrderItem> orderItems, Long usedPointAmount, String orderNumber) {
		validateUsedPointAmount(totalPrice, usedPointAmount);
		this.member = member;
		this.totalPrice = totalPrice;
		this.usedPointAmount = usedPointAmount;
		this.orderNumber = orderNumber;
		orderItems.forEach(this::addOrderItem);
	}

	public Long getMemberId() {
		return member.getId();
	}

	public void addOrderItem(OrderItem orderItem) {
		this.orderItems.add(orderItem);
		orderItem.setOrder(this);
	}

	public String getOrderName() {
		if (orderItems.isEmpty()) return "주문";
		String firstName = orderItems.get(0).getProductName();
		if (orderItems.size() == 1) return firstName;
		return firstName + " 외 " + (orderItems.size() - 1) + "건";
	}

	public void markAsConfirmed() {
		changeStatus(OrderStatus.CONFIRMED);
	}

	public void markAsCancelled() {
		changeStatus(OrderStatus.CANCELED);
	}

	// 주문 상태 변경 로직
	private void changeStatus(OrderStatus newStatus) {
		if (!this.status.canTransitTo(newStatus)) {
			throw new BusinessException(OrderErrorCode.INVALID_ORDER_STATUS);
		}
		this.status = newStatus;
	}

	private void validateUsedPointAmount(Long totalPrice, Long usedPointAmount) {

		if (usedPointAmount < 0) {
			throw new BusinessException(
				OrderErrorCode.INVALID_POINT_AMOUNT,
				"사용 포인트는 0 이상이어야 합니다."
			);
		}

		if (usedPointAmount > totalPrice) {
			throw new BusinessException(
				OrderErrorCode.INVALID_POINT_AMOUNT,
				"사용 포인트는 주문 금액을 초과할 수 없습니다."
			);
		}
	}
}
