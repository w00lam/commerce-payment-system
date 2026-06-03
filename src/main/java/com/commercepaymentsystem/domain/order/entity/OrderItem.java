package com.commercepaymentsystem.domain.order.entity;

import com.commercepaymentsystem.domain.product.entity.Product;
import com.commercepaymentsystem.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id", nullable = false)
	private Order order;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@Column(name = "product_name", nullable = false, length = 200)
	private String productName;

	@Column(name = "order_price", nullable = false, columnDefinition = "int UNSIGNED")
	private Long orderPrice;

	@Column(nullable = false, columnDefinition = "int UNSIGNED")
	private Long quantity;

	public OrderItem(Product product, Long orderPrice, Long quantity) {
		this.product = product;
		this.productName = product.getName();
		this.orderPrice = orderPrice;
		this.quantity = quantity;
	}

	void setOrder(Order order) {
		this.order = order;
	}

	public Long getSubtotal() {
		return orderPrice * quantity;
	}

	public Long getProductId() {
		return this.product.getId();
	}

}
