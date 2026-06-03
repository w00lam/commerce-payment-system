package com.commercepaymentsystem.domain.order.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commercepaymentsystem.domain.cart.entity.CartItem;
import com.commercepaymentsystem.domain.member.entity.Member;
import com.commercepaymentsystem.domain.order.entity.Order;
import com.commercepaymentsystem.domain.order.entity.OrderItem;
import com.commercepaymentsystem.domain.order.repository.OrderRepository;
import com.commercepaymentsystem.domain.product.entity.Product;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

	private final OrderRepository orderRepository;
	private final OrderNumberGenerator orderNumberGenerator;



	@Transactional
	public Order createOrder(Member member, List<CartItem> cartItems, List<Product> products, Long usedPointAmount) {
		Map<Long, Product> productMap = products.stream()
			.collect(Collectors.toMap(Product::getId, Function.identity()));
		List<OrderItem> orderItems = cartItems.stream()
			.map(cartItem -> {
				Product product = productMap.get(cartItem.getProductId());
				return new OrderItem(product, product.getPrice(), cartItem.getQuantity());
			})
			.toList();
		long totalPrice = orderItems.stream().mapToLong(OrderItem::getSubtotal).sum();
		Order order = new Order(member, totalPrice, orderItems, usedPointAmount, orderNumberGenerator.generate());
		return orderRepository.save(order);
	}

	// 주문 상태 변경 (CONFIRMED)
	@Transactional
	public void confirmOrder(Order order) {
		order.markAsConfirmed();
	}

	// 주문 상태 변경 (CANCELLED)
	@Transactional
	public void cancelOrder(Order order) {
		order.markAsCancelled();
	}
}
