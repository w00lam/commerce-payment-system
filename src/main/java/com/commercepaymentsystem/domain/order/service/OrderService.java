package com.commercepaymentsystem.domain.order.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commercepaymentsystem.domain.cart.entity.CartItem;
import com.commercepaymentsystem.domain.member.entity.Member;
import com.commercepaymentsystem.domain.order.dto.GetOrderResponse;
import com.commercepaymentsystem.domain.order.entity.Order;
import com.commercepaymentsystem.domain.order.entity.OrderItem;
import com.commercepaymentsystem.domain.order.exception.OrderErrorCode;
import com.commercepaymentsystem.domain.order.repository.OrderRepository;
import com.commercepaymentsystem.domain.product.entity.Product;
import com.commercepaymentsystem.global.exception.BusinessException;
import com.commercepaymentsystem.global.response.PageResponse;

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

	// 내 주문 목록 조회
	public PageResponse<GetOrderResponse> getProducts(Long memberId, Pageable pageable) {
		Page<Order> orders = orderRepository.findByMember_Id(memberId, pageable);

		return PageResponse.from(orders.map(GetOrderResponse::from));
	}

	public Order getMyOrderDetail(Long orderId, Long memberId) {
		return orderRepository.findByIdAndMemberIdWithOrderItems(orderId, memberId).orElseThrow(
			() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND)
		);
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
