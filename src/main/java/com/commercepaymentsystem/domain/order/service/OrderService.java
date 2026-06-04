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
				return new OrderItem(product, product.getPrice(), cartItem.getQuantity(), cartItem.getId());
			})
			.toList();
		long totalPrice = orderItems.stream().mapToLong(OrderItem::getSubtotal).sum();
		Order order = new Order(member, totalPrice, orderItems, usedPointAmount, orderNumberGenerator.generate());
		return orderRepository.save(order);
	}

	/**
	 * 특정 회원의 주문 전체 내역 목록을 페이징 처리하여 조회합니다.
	 *
	 * @param memberId 회원 식별자
	 * @param pageable 페이징 및 정렬 정보
	 * @return 페이징 처리된 주문 정보 목록 GetOrderResponse를 담은 PageResponse 객체
	 */
	public PageResponse<GetOrderResponse> getOrders(Long memberId, Pageable pageable) {
		Page<Order> orders = orderRepository.findByMember_Id(memberId, pageable);

		return PageResponse.from(orders.map(GetOrderResponse::from));
	}

	/**
	 * 특정 회원의 주문 단건 상세 내역을 주문 상품 정보와 함께 조회합니다.
	 * N+1 문제를 방지하기 위해 Fetch Join 쿼리를 수행합니다.
	 *
	 * @param orderId  주문 식별자
	 * @param memberId 회원 식별자
	 * @return 주문 상품을 포함한 Order 엔티티 객체
	 */
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

	/**
	 * 결제 승인 후처리 단계에서 주문과 주문 상품 정보를 함께 조회하기 위한 메서드입니다.
	 * 지연 로딩 예외 방지 및 쿼리 최적화를 위해 Fetch Join 쿼리를 사용합니다.
	 *
	 * @param orderId 주문 식별자
	 * @return 주문 상품 정보를 담고 있는 Order 엔티티 객체
	 */
	@Transactional(readOnly = true)
	public Order getOrderByIdWithOrderItems(Long orderId) {
		return orderRepository.findWithOrderItemsById(orderId)
			.orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));
	}

	public void validateOwner(Order order, Long memberId) {
		if (!Objects.equals(order.getMemberId(), memberId)) {
			throw new BusinessException(OrderErrorCode.ORDER_OWNER_MISMATCH);
		}
	}

	/**
	 * 특정 회원의 주문 취소를 처리하기 위해 주문 상세 정보를 비관적 락으로 조회합니다.
	 * N+1 문제를 방지하기 위해 주문 상품 및 상품 정보를 Fetch Join 쿼리로 함께 락킹합니다.
	 *
	 * @param orderId  주문 식별자
	 * @param memberId 회원 식별자
	 * @return 비관적 락이 적용된 Order 엔티티 객체
	 */
	@Transactional
	public Order getMyOrderDetailForUpdate(Long orderId, Long memberId) {
		return orderRepository.findByIdAndMemberIdForUpdate(orderId, memberId)
			.orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));
	}

	/**
	 * 환불 및 취소 요청된 주문 상품들의 정보를 바탕으로, 실제 복구해야 할 상품별 복구 수량 맵을 계산하여 반환합니다.
	 *
	 * @param order            주문 엔티티 객체
	 * @param refundQuantities 주문 상품 식별자(ID)와 복구 대상 수량의 매핑 정보
	 * @return 상품 식별자(ID)와 최종 복구할 상품 수량의 매핑 정보 (Map)
	 */
	@Transactional
	public Map<Long, Long> restoreProductStock(Order order, Map<Long, Long> refundQuantities) {
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
		return productQuantities;
	}
}
