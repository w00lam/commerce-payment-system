package com.commercepaymentsystem.domain.order.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commercepaymentsystem.domain.member.entity.Member;
import com.commercepaymentsystem.domain.order.entity.Order;
import com.commercepaymentsystem.domain.order.entity.OrderItem;
import com.commercepaymentsystem.domain.order.repository.OrderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

	private final OrderRepository orderRepository;
	private final OrderNumberGenerator orderNumberGenerator;

	// 주문 생성
	@Transactional
	public Order createOrder(Member member, List<OrderItem> orderItems, Long totalPrice, Long usedPointAmount) {

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
