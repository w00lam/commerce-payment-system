package com.commercepaymentsystem.domain.order.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.commercepaymentsystem.domain.cart.entity.CartItem;
import com.commercepaymentsystem.domain.cart.exception.CartErrorCode;
import com.commercepaymentsystem.domain.cart.service.CartService;
import com.commercepaymentsystem.domain.member.entity.Member;
import com.commercepaymentsystem.domain.member.service.MemberService;
import com.commercepaymentsystem.domain.order.dto.OrderCreateRequest;
import com.commercepaymentsystem.domain.order.dto.OrderCreateResponse;
import com.commercepaymentsystem.domain.order.dto.OrderPreviewRequest;
import com.commercepaymentsystem.domain.order.dto.OrderPreviewResponse;
import com.commercepaymentsystem.domain.order.entity.Order;
import com.commercepaymentsystem.domain.order.entity.OrderItem;
import com.commercepaymentsystem.domain.payment.dto.PaymentCreateCommand;
import com.commercepaymentsystem.domain.payment.dto.PaymentCreateResult;
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

		// 1. 장바구니 조회 (선택된 아이템만)
		List<CartItem> cartItems = getValidateCartItems(memberId, cartItemIds);

		// 2~3. 재고 차감 + 스냅샷 OrderItem 생성
		List<OrderItem> orderItems = new ArrayList<>();

		for (CartItem cartItem : cartItems) {
			Long productId = cartItem.getProductId();
			Product product = productService.getProduct(productId);

			if (product.getStatus() != ProductStatus.ON_SALE) {
				throw new BusinessException(ProductErrorCode.PRODUCT_NOT_ON_SALE);
			}
			product.removeStock(cartItem.getQuantity());

			OrderItem orderItem = new OrderItem(
				product,
				product.getPrice(),
				cartItem.getQuantity()
			);
			orderItems.add(orderItem);
		}
		long totalPrice = orderItems.stream().mapToLong(OrderItem::getSubtotal).sum();

		member.deductPoint(request.safeUsedPointAmount());

		// 4. 주문 저장
		Order order = orderService.createOrder(member, orderItems, totalPrice, request.safeUsedPointAmount());

		// 5. 결제 코맨드 생성
		PaymentCreateCommand paymentCreateCommand = new PaymentCreateCommand(memberId, order.getId(), totalPrice,
			order.getUsedPointAmount(), totalPrice - order.getUsedPointAmount());

		// 6. 결제 정보 생성
		PaymentCreateResult paymentCreateResult = paymentService.createPendingPayment(paymentCreateCommand);

		// 7. 주문한 장바구니 아이템만 삭제
		List<Long> orderedItemIds = cartItems.stream().map(CartItem::getId).toList();
		cartService.clearCartItems(orderedItemIds, memberId);

		// 8. 응답
		return new OrderCreateResponse(
			paymentCreateResult.orderId(),
			order.getOrderNumber(),
			paymentCreateResult.memberId(),
			paymentCreateResult.totalOrderAmount(),
			paymentCreateResult.usedPointAmount(),
			paymentCreateResult.finalPaymentAmount(),
			order.getStatus(),
			paymentCreateResult.paymentId(),
			paymentCreateResult.status(),
			orderItems
		);
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
			throw new BusinessException(CartErrorCode.CART_ITEM_NOT_FOUND);
		}

		if (!distinctCartItemIds.isEmpty() && cartItems.size() != distinctCartItemIds.size()) {
			throw new BusinessException(CartErrorCode.CART_ITEM_NOT_FOUND);
		}

		return cartItems;
	}
}
