package com.commercepaymentsystem.domain.order.facade;

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
import com.commercepaymentsystem.domain.order.service.OrderService;
import com.commercepaymentsystem.domain.payment.dto.PaymentConfirmCommand;
import com.commercepaymentsystem.domain.payment.dto.PaymentConfirmResult;
import com.commercepaymentsystem.domain.payment.dto.PaymentCreateCommand;
import com.commercepaymentsystem.domain.payment.dto.PaymentCreateResult;
import com.commercepaymentsystem.domain.payment.entity.Payment;
import com.commercepaymentsystem.domain.payment.entity.PaymentStatus;
import com.commercepaymentsystem.domain.payment.facade.PaymentConfirmFacade;
import com.commercepaymentsystem.domain.payment.service.PaymentService;
import com.commercepaymentsystem.domain.product.entity.Product;
import com.commercepaymentsystem.domain.product.entity.ProductStatus;
import com.commercepaymentsystem.domain.product.exception.ProductErrorCode;
import com.commercepaymentsystem.domain.product.service.ProductService;
import com.commercepaymentsystem.global.exception.BusinessException;
import com.commercepaymentsystem.global.exception.GlobalErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderFacade {

	private final CartService cartService;
	private final MemberService memberService;
	private final OrderService orderService;
	private final PaymentConfirmFacade paymentConfirmFacade;
	private final PaymentService paymentService;
	private final ProductService productService;

	public OrderPreviewResponse previewOrder(Long memberId, OrderPreviewRequest request) {
		List<Long> cartItemIds = request != null ? request.cartItemIds() : List.of();
		List<CartItem> cartItems = getPreviewCartItems(
			memberId,
			cartItemIds != null ? cartItemIds : List.of()
		);

		if (cartItems.isEmpty()) {
			return new OrderPreviewResponse(
				memberId,
				0L,
				List.of()
			);
		}

		List<OrderPreviewResponse.CheckoutItemResponse> items = cartItems.stream()
			.map(cartItem -> {
				Product product = productService.getProduct(cartItem.getProductId());
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

		long totalPrice = items.stream()
			.mapToLong(OrderPreviewResponse.CheckoutItemResponse::subtotal)
			.sum();

		return new OrderPreviewResponse(memberId, totalPrice, items);
	}

	/**
	 * 장바구니 항목으로 주문과 결제 대기 건을 생성합니다.
	 *
	 * <p>포인트 사용 금액은 회원 row를 비관적 락으로 조회한 뒤 검증해, 동시에 여러 주문이
	 * 같은 포인트 잔액을 초과 사용하지 못하게 합니다.</p>
	 */
	@Transactional
	public OrderCreateResponse createOrder(Long memberId, OrderCreateRequest request) {
		if (request == null) {
			throw new BusinessException(GlobalErrorCode.INVALID_INPUT_VALUE);
		}

		List<Long> cartItemIds = request.cartItemIds();

		Member member = memberService.getMemberForUpdate(memberId);
		long usedPointAmount = request.safeUsedPointAmount();
		if (member.getPointBalance() < usedPointAmount) {
			throw new BusinessException(MemberErrorCode.POINT_NOT_ENOUGH, "포인트 잔액이 부족합니다.");
		}

		List<CartItem> cartItems = getValidateCartItems(memberId, cartItemIds);
		List<Product> lockedProducts = productService.deductProductStocks(cartItems);
		Order order = orderService.createOrder(member, cartItems, lockedProducts, usedPointAmount);

		PaymentCreateResult paymentCreateResult = paymentService.createPendingPayment(PaymentCreateCommand.from(order));
		PaymentStatus paymentStatus = confirmPointOnlyPaymentIfNeeded(paymentCreateResult, memberId);

		List<OrderItemCreateResponse> items = order.getOrderItems().stream()
			.map(OrderItemCreateResponse::from)
			.toList();

		return new OrderCreateResponse(
			order.getId(),
			order.getOrderNumber(),
			paymentOrderName(order),
			order.getMemberId(),
			order.getTotalPrice(),
			order.getUsedPointAmount(),
			paymentCreateResult.finalPaymentAmount(),
			order.getStatus(),
			paymentCreateResult.paymentId(),
			paymentStatus,
			items
		);
	}

	private PaymentStatus confirmPointOnlyPaymentIfNeeded(PaymentCreateResult paymentCreateResult, Long memberId) {
		if (paymentCreateResult.finalPaymentAmount() > 0) {
			return paymentCreateResult.status();
		}

		PaymentConfirmResult confirmResult = paymentConfirmFacade.confirm(
			PaymentConfirmCommand.of(paymentCreateResult.paymentId(), memberId)
		);
		return confirmResult.status();
	}

	private String paymentOrderName(Order order) {
		return order.getOrderName();
	}

	/**
	 * 회원의 주문 상세 내역과 결제 정보를 함께 조회합니다.
	 */
	public GetOrderDetailResponse getOrderDetail(Long memberId, Long orderId) {
		Order order = orderService.getMyOrderDetail(orderId, memberId);
		PaymentCreateResult payment = paymentService.findPaymentByOrderId(order.getId())
			.orElse(null);

		return GetOrderDetailResponse.of(order, payment);
	}

	/**
	 * 아직 결제가 완료되지 않은 주문을 취소하고 차감된 재고를 복구합니다.
	 */
	@Transactional
	public OrderCancelResponse cancelOrder(Long memberId, Long orderId) {
		Payment payment = paymentService.getPendingPaymentByOrderIdForUpdate(
			orderId,
			memberId
		);

		Order order = orderService.getMyOrderDetailForUpdate(orderId, memberId);

		orderService.cancelOrder(order);
		paymentService.failPayment(payment);

		Map<Long, Long> productQuantities = order.getOrderItems().stream()
			.collect(Collectors.toMap(
				orderItem -> orderItem.getProduct().getId(),
				OrderItem::getQuantity,
				Long::sum
			));

		productService.restoreProductStocks(productQuantities);

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
