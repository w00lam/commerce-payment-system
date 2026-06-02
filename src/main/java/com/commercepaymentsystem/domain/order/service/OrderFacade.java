package com.commercepaymentsystem.domain.order.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.commercepaymentsystem.domain.cart.entity.CartItem;
import com.commercepaymentsystem.domain.cart.exception.CartErrorCode;
import com.commercepaymentsystem.domain.cart.service.CartItemCommand;
import com.commercepaymentsystem.domain.cart.service.CartService;
import com.commercepaymentsystem.domain.member.entity.Member;
import com.commercepaymentsystem.domain.member.service.MemberCommand;
import com.commercepaymentsystem.domain.order.dto.OrderCreateRequest;
import com.commercepaymentsystem.domain.order.dto.OrderCreateResponse;
import com.commercepaymentsystem.domain.order.entity.Order;
import com.commercepaymentsystem.domain.order.entity.OrderItem;
import com.commercepaymentsystem.domain.order.exception.OrderErrorCode;
import com.commercepaymentsystem.domain.order.mapper.OrderCreateMapper;
import com.commercepaymentsystem.domain.order.repository.OrderItemRepository;
import com.commercepaymentsystem.domain.order.repository.OrderRepository;
import com.commercepaymentsystem.domain.payment.dto.PaymentCreateCommand;
import com.commercepaymentsystem.domain.payment.dto.PaymentCreateResult;
import com.commercepaymentsystem.domain.payment.service.PaymentService;
import com.commercepaymentsystem.domain.point.exception.PointErrorCode;
import com.commercepaymentsystem.domain.product.entity.Product;
import com.commercepaymentsystem.domain.product.entity.ProductStatus;
import com.commercepaymentsystem.domain.product.exception.ProductErrorCode;
import com.commercepaymentsystem.domain.product.service.ProductCommand;
import com.commercepaymentsystem.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OrderFacade {

	private final MemberCommand memberCommand;
	private final CartItemCommand cartItemCommand;
	private final ProductCommand productCommand;
	private final CartService cartService;
	private final OrderRepository orderRepository;
	private final OrderItemRepository orderItemRepository;
	private final OrderNumberGenerator orderNumberGenerator;
	private final PaymentService paymentService;

	@Transactional
	public OrderCreateResponse createOrder(
		Long memberId,
		OrderCreateRequest request
	) {
		Member member = memberCommand.getMember(memberId);

		List<CartItem> cartItems = findOrderCartItems(
			member.getId(),
			request.cartItemIds()
		);

		if (cartItems.isEmpty()) {
			throw new BusinessException(OrderErrorCode.EMPTY_ORDER_ITEM);
		}

		Map<Long, Product> productMap = findProductMap(cartItems);

		validateAllProductsExist(
			cartItems,
			productMap
		);

		validateOrderableProduct(
			cartItems,
			productMap
		);

		Long totalAmount = calculateTotalAmount(
			cartItems,
			productMap
		);

		Long usedPointAmount = request.safeUsedPointAmount();

		validateUsedPointAmount(
			totalAmount,
			usedPointAmount
		);

		usePointIfRequested(
			member,
			usedPointAmount
		);

		decreaseProductStock(
			cartItems,
			productMap
		);

		Order order = saveOrder(
			member,
			totalAmount,
			usedPointAmount
		);

		List<OrderItem> orderItems = saveOrderItems(
			order,
			cartItems,
			productMap
		);

		PaymentCreateResult payment = createReadyPayment(
			member.getId(),
			order.getId(),
			totalAmount,
			usedPointAmount
		);

		cartService.clearCart(member.getId());

		return OrderCreateMapper.toResponse(
			order,
			payment,
			orderItems
		);
	}

	private List<CartItem> findOrderCartItems(
		Long memberId,
		List<Long> cartItemIds
	) {
		if (cartItemIds == null || cartItemIds.isEmpty()) {
			return cartItemCommand.getCartItemsByMemberId(memberId);
		}

		List<Long> distinctCartItemIds = cartItemIds.stream()
			.distinct()
			.toList();

		List<CartItem> cartItems = cartItemCommand.getCartItemsByMemberIdAndIds(
			memberId,
			distinctCartItemIds
		);

		if (cartItems.size() != distinctCartItemIds.size()) {
			throw new BusinessException(CartErrorCode.CART_ITEM_NOT_FOUND);
		}

		return cartItems;
	}

	private Map<Long, Product> findProductMap(List<CartItem> cartItems) {
		List<Long> productIds = cartItems.stream()
			.map(CartItem::getProductId)
			.distinct()
			.toList();

		return productCommand.getProductsForOrderCreate(productIds);
	}

	private void validateAllProductsExist(
		List<CartItem> cartItems,
		Map<Long, Product> productMap
	) {
		long productCount = cartItems.stream()
			.map(CartItem::getProductId)
			.distinct()
			.count();

		if (productMap.size() != productCount) {
			throw new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND);
		}
	}

	private void validateOrderableProduct(
		List<CartItem> cartItems,
		Map<Long, Product> productMap
	) {
		for (CartItem cartItem : cartItems) {
			Product product = getProduct(
				productMap,
				cartItem.getProductId()
			);

			validateProductStatus(product);
			validateEnoughStock(
				product,
				cartItem.getQuantity()
			);
		}
	}

	private void validateProductStatus(Product product) {
		if (product.getStatus() != ProductStatus.ON_SALE) {
			throw new BusinessException(ProductErrorCode.PRODUCT_NOT_ON_SALE);
		}
	}

	private void validateEnoughStock(
		Product product,
		Long quantity
	) {
		if (quantity == null || quantity <= 0) {
			throw new BusinessException(ProductErrorCode.INVALID_QUANTITY);
		}

		if (product.getStock() < quantity) {
			throw new BusinessException(ProductErrorCode.OUT_OF_STOCK);
		}
	}

	private Long calculateTotalAmount(
		List<CartItem> cartItems,
		Map<Long, Product> productMap
	) {
		return cartItems.stream()
			.mapToLong(cartItem -> {
				Product product = getProduct(
					productMap,
					cartItem.getProductId()
				);

				return product.getPrice() * cartItem.getQuantity();
			})
			.sum();
	}

	private void validateUsedPointAmount(
		Long totalAmount,
		Long usedPointAmount
	) {
		if (usedPointAmount == null || usedPointAmount < 0) {
			throw new BusinessException(PointErrorCode.INVALID_POINT_AMOUNT);
		}

		if (usedPointAmount > totalAmount) {
			throw new BusinessException(PointErrorCode.INSUFFICIENT_POINT);
		}
	}

	private void usePointIfRequested(
		Member member,
		Long usedPointAmount
	) {
		if (usedPointAmount == 0) {
			return;
		}

		member.usePoint(usedPointAmount);
	}

	private void decreaseProductStock(
		List<CartItem> cartItems,
		Map<Long, Product> productMap
	) {
		for (CartItem cartItem : cartItems) {
			Product product = getProduct(
				productMap,
				cartItem.getProductId()
			);

			product.removeStock(cartItem.getQuantity());
		}
	}

	private Order saveOrder(
		Member member,
		Long totalAmount,
		Long usedPointAmount
	) {
		Order order = Order.create(
			member,
			orderNumberGenerator.generate(),
			totalAmount,
			usedPointAmount
		);

		return orderRepository.save(order);
	}

	private List<OrderItem> saveOrderItems(
		Order order,
		List<CartItem> cartItems,
		Map<Long, Product> productMap
	) {
		List<OrderItem> orderItems = cartItems.stream()
			.map(cartItem -> {
				Product product = getProduct(
					productMap,
					cartItem.getProductId()
				);

				return OrderItem.create(
					order,
					product.getId(),
					product.getName(),
					product.getPrice(),
					cartItem.getQuantity()
				);
			})
			.toList();

		return orderItemRepository.saveAll(orderItems);
	}

	private PaymentCreateResult createReadyPayment(
		Long memberId,
		Long orderId,
		Long totalAmount,
		Long usedPointAmount
	) {
		Long finalPaymentAmount = totalAmount - usedPointAmount;

		PaymentCreateCommand command = new PaymentCreateCommand(
			memberId,
			orderId,
			totalAmount,
			usedPointAmount,
			finalPaymentAmount
		);

		return paymentService.createPendingPayment(command);
	}

	private Product getProduct(
		Map<Long, Product> productMap,
		Long productId
	) {
		Product product = productMap.get(productId);

		if (product == null) {
			throw new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND);
		}

		return product;
	}
}