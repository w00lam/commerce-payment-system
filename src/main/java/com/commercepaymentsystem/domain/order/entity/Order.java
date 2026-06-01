package com.commercepaymentsystem.domain.order.entity;

import com.commercepaymentsystem.domain.member.entity.Member;
import com.commercepaymentsystem.global.entity.BaseEntity;
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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// 주문 회원
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	// 사용자에게 노출되는 주문번호
	@Column(nullable = false, unique = true, length = 50)
	private String orderNumber;

	// 총 주문 금액
	@Column(nullable = false)
	private Integer totalAmount;

	// 사용 포인트 금액
	@Column(nullable = false)
	private Integer usedPointAmount;

	// 주문 상태
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private OrderStatus status;

	private Order (
		Member member,
		String orderNumber,
		Integer totalAmount,
		Integer usedPointAmount
	) {
		this.member = member;
		this.orderNumber = orderNumber;
		this.totalAmount = totalAmount;
		this.usedPointAmount = usedPointAmount;
		this.status = OrderStatus.CREATED;
	}

	public static Order create(
		Member member,
		String orderNumber,
		Integer totalAmount,
		Integer usedPointAmount
	) {
		return new Order(
			member,
			orderNumber,
			totalAmount,
			usedPointAmount
		);
	}
}