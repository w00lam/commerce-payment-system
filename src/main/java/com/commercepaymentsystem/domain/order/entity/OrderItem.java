package com.commercepaymentsystem.domain.order.entity;

import com.commercepaymentsystem.global.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "order_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id", nullable = false)
	private Order order;

	@Column(name = "product_id", nullable = false)
	private Long productId;

	@Column(name = "product_name", nullable = false, length = 100)
	private String productName;

	@Column(name = "order_price", nullable = false, columnDefinition = "INT UNSIGNED")
	private Long orderPrice;

	@Column(nullable = false, columnDefinition = "INT UNSIGNED")
	private Long quantity;

	private OrderItem(
		Order order,
		Long productId,
		String productName,
		Long orderPrice,
		Long quantity
	) {
		this.order = order;
		this.productId = productId;
		this.productName = productName;
		this.orderPrice = orderPrice;
		this.quantity = quantity;
	}

	public static OrderItem create(
		Order order,
		Long productId,
		String productName,
		Long orderPrice,
		Long quantity
	) {
		return new OrderItem(
			order,
			productId,
			productName,
			orderPrice,
			quantity
		);
	}

	public Long getTotalPrice() {
		return this.orderPrice * this.quantity;
	}
}