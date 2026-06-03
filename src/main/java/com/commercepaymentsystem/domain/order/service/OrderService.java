package com.commercepaymentsystem.domain.order.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
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
	public PageResponse<GetOrderResponse> getOrders(Long memberId, Pageable pageable) {
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

	@Transactional(readOnly = true)
	public Order getOrderById(Long orderId) {
		return orderRepository.findById(orderId)
			.orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));
	}

	public void validateOwner(Order order, Long memberId) {
		if (!Objects.equals(order.getMemberId(), memberId)) {
			throw new BusinessException(OrderErrorCode.ORDER_OWNER_MISMATCH);
		}
	}

	/**
	 * 환불 처리 시 주문 상품 정보를 바탕으로 상품 식별자를 추출하고 ProductService를 호출하여 비관적 락 기반의 안전한 재고 복구를 수행합니다.
	 *
	 * @param order 주문 엔티티 객체
	 * @param refundQuantities 주문 상품 식별자(ID)와 복구할 환불 수량의 매핑 정보
	 */
	@Transactional
	public void restoreProductStock(Order order, Map<Long, Long> refundQuantities) {
		Map<Long, OrderItem> orderItems = order.getOrderItems().stream()
			.collect(Collectors.toMap(OrderItem::getId, Function.identity()));
		Map<Long, Long> productQuantities = new java.util.HashMap<>();
		for (Map.Entry<Long, Long> refundQuantity : refundQuantities.entrySet()) {
			OrderItem orderItem = orderItems.get(refundQuantity.getKey());
			if (orderItem == null) {
				throw new BusinessException(OrderErrorCode.ORDER_ITEM_NOT_FOUND);
			}
			productQuantities.merge(orderItem.getProductId(), refundQuantity.getValue(), Long::sum);
		}
		productService.restoreProductStocks(productQuantities);
	}
}
