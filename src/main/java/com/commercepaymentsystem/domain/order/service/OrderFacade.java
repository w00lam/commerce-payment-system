package com.commercepaymentsystem.domain.order.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.commercepaymentsystem.domain.cart.entity.CartItem;
import com.commercepaymentsystem.domain.cart.exception.CartErrorCode;
import com.commercepaymentsystem.domain.cart.service.CartService;
import com.commercepaymentsystem.domain.member.entity.Member;
import com.commercepaymentsystem.domain.member.exception.MemberErrorCode;
import com.commercepaymentsystem.domain.member.service.MemberService;
import com.commercepaymentsystem.domain.order.dto.GetOrderDetailResponse;
import com.commercepaymentsystem.domain.order.dto.OrderCancelResponse;
import com.commercepaymentsystem.domain.order.dto.OrderCreateRequest;
import com.commercepaymentsystem.domain.order.dto.OrderCreateResponse;
import com.commercepaymentsystem.domain.order.dto.OrderItemCreateResponse;
import com.commercepaymentsystem.domain.order.dto.OrderPreviewRequest;
import com.commercepaymentsystem.domain.order.dto.OrderPreviewResponse;
import com.commercepaymentsystem.domain.order.entity.Order;
import com.commercepaymentsystem.domain.order.entity.OrderItem;
import com.commercepaymentsystem.domain.order.exception.OrderErrorCode;
import com.commercepaymentsystem.domain.payment.dto.PaymentCreateCommand;
import com.commercepaymentsystem.domain.payment.dto.PaymentCreateResult;
import com.commercepaymentsystem.domain.payment.entity.Payment;
import com.commercepaymentsystem.domain.payment.service.PaymentService;
import com.commercepaymentsystem.domain.product.entity.Product;
import com.commercepaymentsystem.domain.product.entity.ProductStatus;
import com.commercepaymentsystem.domain.product.exception.ProductErrorCode;
import com.commercepaymentsystem.domain.product.service.ProductService;
import com.commercepaymentsystem.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderFacade {

	private final CartService cartService;
	private final MemberService memberService;
	private final OrderService orderService;
	private final PaymentService paymentService;
	private final ProductService productService;

	public OrderPreviewResponse previewOrder(Long memberId, OrderPreviewRequest request) {

		List<Long> cartItemIds = request.cartItemIds();

		// 주문서 미리보기: 재고 차감/주문 생성 없는 읽기 전용
		// cartItemIds가 null/비어있으면 "전체 장바구니", 값이 있으면 "선택된 아이템만" 주문서에 담는다.
		List<CartItem> cartItems = getPreviewCartItems(
			memberId,
			cartItemIds != null ? cartItemIds : List.of()
		);

		// 만약 장바구니가 비어있다면 주문서 미리보기에서는 빈 응답을 보냅니다
		if (cartItems.isEmpty()) {
			return new OrderPreviewResponse(
				memberId,
				0L,
				List.of()
			);
		}

		// 장바구니 아이템에서 상품 가격과 장바구니 수량을 곱해서 각 아이템의 총액을 구한다.
		// CartItem을 OrderPreviewResponse.CheckoutItemResponse로 변환
		List<OrderPreviewResponse.CheckoutItemResponse> items = cartItems.stream()
			.map(cartItem -> {
				Long productId = cartItem.getProductId();
				Product product = productService.getProduct(productId);
				if (product.getStatus() != ProductStatus.ON_SALE) {
					throw new BusinessException(ProductErrorCode.PRODUCT_NOT_ON_SALE);
				}

				Long price = product.getPrice();
				long subtotal = price * cartItem.getQuantity();
				return new OrderPreviewResponse.CheckoutItemResponse(
					cartItem.getProductId(),
					product.getName(),
					price,
					cartItem.getQuantity(),
					subtotal
				);
			})
			.toList();

		// 장바구니 주문 총액을 구한다. OrderPreviewResponse.CheckoutItemResponse의 subtotal을 모두 더한다.
		long totalPrice = items.stream()
			.mapToLong(OrderPreviewResponse.CheckoutItemResponse::subtotal)
			.sum();

		return new OrderPreviewResponse(memberId, totalPrice, items);
	}

	@Transactional
	public OrderCreateResponse createOrder(Long memberId, OrderCreateRequest request) {
		List<Long> cartItemIds = (request != null) ? request.cartItemIds() : List.of();

		// 0. 회원 조회
		Member member = memberService.getMember(memberId);
		long usedPointAmount = request.safeUsedPointAmount();
		if (member.getPointBalance() < usedPointAmount) {
			throw new BusinessException(MemberErrorCode.POINT_NOT_ENOUGH, "포인트 잔액이 부족합니다");
		}

		// 1. 장바구니 조회 (선택된 아이템만)
		List<CartItem> cartItems = getValidateCartItems(memberId, cartItemIds);

		// 2. 상품 리스트 생성
		List<Product> lockedProducts = productService.deductProductStocks(cartItems);

		// 3. 주문 저장
		Order order = orderService.createOrder(member, cartItems, lockedProducts, usedPointAmount);

		// 4. 결제 정보 생성
		PaymentCreateCommand command = PaymentCreateCommand.from(order);
		PaymentCreateResult paymentCreateResult = paymentService.createPendingPayment(command);

		// 5. 응답 반환
		List<OrderItemCreateResponse> items = order.getOrderItems().stream()
			.map(OrderItemCreateResponse::from)
			.toList();

		return new OrderCreateResponse(
			order.getId(),
			order.getOrderNumber(),
			order.getMemberId(),
			order.getTotalPrice(),
			order.getUsedPointAmount(),
			paymentCreateResult.finalPaymentAmount(),
			order.getStatus(),
			paymentCreateResult.paymentId(),
			paymentCreateResult.status(),
			items
		);
	}

	public GetOrderDetailResponse getOrderDetail(Long memberId, Long orderId) {

		Order order = orderService.getMyOrderDetail(orderId, memberId);
		PaymentCreateResult payment = paymentService.findPaymentByOrderId(order.getId())
			.orElse(null);

		return GetOrderDetailResponse.of(order, payment);
	}

	@Transactional
	public OrderCancelResponse cancelOrder(Long memberId, Long orderId) {
		Order order = orderService.getMyOrderDetailForUpdate(orderId, memberId);

		Payment payment = paymentService.getPendingPaymentByOrderIdForUpdate(
			order.getId(),
			memberId
		);

		orderService.cancelOrder(order);
		paymentService.failPayment(payment);

		Map<Long, Long> orderItemQuantities = order.getOrderItems().stream()
			.collect(Collectors.toMap(
				OrderItem::getId,
				OrderItem::getQuantity,
				Long::sum
			));

		productService.restoreProductStocks(orderItemQuantities);
		return OrderCancelResponse.from(order);
	}

	private List<CartItem> getPreviewCartItems(
		Long memberId,
		List<Long> cartItemIds
	) {
		List<Long> distinctCartItemIds = cartItemIds.stream()
			.distinct()
			.toList();

		List<CartItem> cartItems = distinctCartItemIds.isEmpty()
			? cartService.findCartEntities(memberId)
			: cartService.findCartEntitiesByIds(memberId, distinctCartItemIds);

		if (!distinctCartItemIds.isEmpty() && cartItems.size() != distinctCartItemIds.size()) {
			throw new BusinessException(CartErrorCode.CART_ITEM_NOT_FOUND);
		}

		return cartItems;
	}

	private List<CartItem> getValidateCartItems(Long memberId, List<Long> cartItemIds) {
		List<Long> distinctCartItemIds = cartItemIds.stream()
			.distinct()
			.toList();

		// distinctCartItemIds 비어있으면 "전체 장바구니", 아니면 "선택된 아이템만" 조회
		List<CartItem> cartItems = distinctCartItemIds.isEmpty()
			? cartService.findCartEntities(memberId)
			: cartService.findCartEntitiesByIds(memberId, distinctCartItemIds);

		if (cartItems.isEmpty()) {
			throw new BusinessException(OrderErrorCode.EMPTY_ORDER_ITEM);
		}

		if (!distinctCartItemIds.isEmpty() && cartItems.size() != distinctCartItemIds.size()) {
			throw new BusinessException(CartErrorCode.CART_ITEM_NOT_FOUND);
		}

		return cartItems;
	}

}
