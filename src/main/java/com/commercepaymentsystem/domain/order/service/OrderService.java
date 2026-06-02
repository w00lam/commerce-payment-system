package com.commercepaymentsystem.domain.order.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commercepaymentsystem.domain.cart.entity.CartItem;
import com.commercepaymentsystem.domain.cart.exception.CartErrorCode;
import com.commercepaymentsystem.domain.cart.service.CartItemCommand;
import com.commercepaymentsystem.domain.cart.service.CartService;
import com.commercepaymentsystem.domain.member.entity.Member;
import com.commercepaymentsystem.domain.member.service.MemberCommand;
import com.commercepaymentsystem.domain.order.dto.OrderCreateRequest;
import com.commercepaymentsystem.domain.order.dto.OrderCreateResponse;
import com.commercepaymentsystem.domain.order.dto.OrderPreviewItemResponse;
import com.commercepaymentsystem.domain.order.dto.OrderPreviewRequest;
import com.commercepaymentsystem.domain.order.dto.OrderPreviewResponse;
import com.commercepaymentsystem.domain.order.entity.Order;
import com.commercepaymentsystem.domain.order.entity.OrderItem;
import com.commercepaymentsystem.domain.order.exception.OrderErrorCode;
import com.commercepaymentsystem.domain.order.mapper.OrderCreateMapper;
import com.commercepaymentsystem.domain.order.mapper.OrderPreviewMapper;
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

@Service
@RequiredArgsConstructor
public class OrderService {

	private final MemberCommand memberCommand;
	private final CartItemCommand cartItemCommand;
	private final ProductCommand productCommand;
	private final CartService cartService;
	private final OrderRepository orderRepository;
	private final OrderItemRepository orderItemRepository;
	private final OrderNumberGenerator orderNumberGenerator;
	private final PaymentService paymentService;

	/**
	 * 장바구니 상품을 기준으로 주문과 결제 대기 정보를 생성합니다.
	 *
	 * <p>cartItemIds가 비어 있으면 회원의 전체 장바구니 상품을 주문 대상으로 사용합니다.
	 * cartItemIds가 있으면 해당 장바구니 상품만 주문 대상으로 사용합니다.
	 * 상품 상태와 재고를 검증한 뒤 포인트와 재고를 차감하고,
	 * 주문, 주문 상품, 결제 대기 정보를 생성합니다.</p>
	 *
	 * <p>주문 생성이 완료되면 장바구니를 비웁니다.
	 * 중간에 예외가 발생하면 전체 트랜잭션이 롤백됩니다.</p>
	 *
	 * @param memberId 회원 ID
	 * @param request 주문 생성 요청
	 * @return 주문 생성 응답
	 */
	@Transactional
	public OrderCreateResponse createOrder(
		Long memberId,
		OrderCreateRequest request
	) {
		Member member = memberCommand.getMember(memberId);

		List<CartItem> cartItems = findPreviewCartItems(
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

	/**
	 * 주문서 미리보기 정보를 조회합니다.
	 *
	 * <p>장바구니 상품을 기준으로 주문 예정 상품 목록과 총 주문 금액을 계산합니다.
	 * 장바구니가 비어 있는 경우에도 예외를 발생시키지 않고 빈 미리보기 응답을 반환합니다.
	 * 이 메서드는 상품 재고를 차감하지 않고, 주문 가능 여부만 검증합니다.</p>
	 *
	 * @param memberId 회원 ID
	 * @param request 주문서 미리보기 요청
	 * @return 주문서 미리보기 응답
	 */
	@Transactional(readOnly = true)
	public OrderPreviewResponse previewOrder(
		Long memberId,
		OrderPreviewRequest request
	) {
		Member member = memberCommand.getMember(memberId);

		List<CartItem> cartItems = findPreviewCartItems(
			member.getId(),
			request.cartItemIds()
		);

		if (cartItems.isEmpty()) {
			return OrderPreviewMapper.toResponse(
				member.getId(),
				0L,
				List.of()
			);
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

		List<OrderPreviewItemResponse> items = cartItems.stream()
			.map(cartItem -> OrderPreviewMapper.toItemResponse(
				cartItem,
				getProduct(
					productMap,
					cartItem.getProductId()
				)
			))
			.toList();

		return OrderPreviewMapper.toResponse(
			member.getId(),
			totalAmount,
			items
		);
	}

	/**
	 * 주문서 미리보기와 주문 생성에 사용할 장바구니 상품 목록을 조회합니다.
	 *
	 * <p>cartItemIds가 비어 있으면 회원의 전체 장바구니 상품을 조회하고,
	 * 값이 있으면 중복을 제거한 뒤 해당 ID 목록에 포함된 장바구니 상품만 조회합니다.
	 * 존재하지 않거나 본인 장바구니 상품이 아닌 ID가 포함되면 예외를 발생시킵니다.</p>
	 *
	 * @param memberId 검증된 회원 ID
	 * @param cartItemIds 장바구니 상품 ID 목록
	 * @return 장바구니 상품 목록
	 */
	private List<CartItem> findPreviewCartItems(
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

	/**
	 * 장바구니 상품에 연결된 상품 목록을 조회합니다.
	 *
	 * @param cartItems 장바구니 상품 목록
	 * @return 상품 ID를 key로 갖는 상품 Map
	 */
	private Map<Long, Product> findProductMap(List<CartItem> cartItems) {
		List<Long> productIds = extractDistinctProductIds(cartItems);

		return productCommand.getProductsForOrder(productIds);
	}

	/**
	 * 장바구니 상품 목록에서 중복 없는 상품 ID 목록을 추출합니다.
	 *
	 * @param cartItems 장바구니 상품 목록
	 * @return 중복 제거된 상품 ID 목록
	 */
	private List<Long> extractDistinctProductIds(List<CartItem> cartItems) {
		return cartItems.stream()
			.map(CartItem::getProductId)
			.distinct()
			.toList();
	}

	/**
	 * 장바구니 상품에 연결된 모든 상품이 존재하는지 검증합니다.
	 *
	 * @param cartItems 장바구니 상품 목록
	 * @param productMap 상품 ID를 key로 갖는 상품 Map
	 */
	private void validateAllProductsExist(
		List<CartItem> cartItems,
		Map<Long, Product> productMap
	) {
		int productCount = extractDistinctProductIds(cartItems).size();

		if (productMap.size() != productCount) {
			throw new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND);
		}
	}

	/**
	 * 상품이 주문 가능한 상태인지 검증합니다.
	 *
	 * <p>판매 중인 상품인지 확인하고, 장바구니 수량보다 재고가 충분한지 검증합니다.
	 * 이 메서드는 재고를 차감하지 않고 주문 가능 여부만 확인합니다.</p>
	 *
	 * @param cartItems 장바구니 상품 목록
	 * @param productMap 상품 ID를 key로 갖는 상품 Map
	 */
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

	/**
	 * 상품이 판매 중 상태인지 검증합니다.
	 *
	 * @param product 상품
	 */
	private void validateProductStatus(Product product) {
		if (product.getStatus() != ProductStatus.ON_SALE) {
			throw new BusinessException(ProductErrorCode.PRODUCT_NOT_ON_SALE);
		}
	}

	/**
	 * 상품 재고가 요청 수량보다 충분한지 검증합니다.
	 *
	 * @param product 상품
	 * @param quantity 요청 수량
	 */
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

	/**
	 * 장바구니 상품 목록을 기준으로 총 주문 예정 금액을 계산합니다.
	 *
	 * @param cartItems 장바구니 상품 목록
	 * @param productMap 상품 ID를 key로 갖는 상품 Map
	 * @return 총 주문 예정 금액
	 */
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

	/**
	 * 사용 포인트 금액을 검증합니다.
	 *
	 * <p>사용 포인트는 음수일 수 없고, 주문 총액을 초과할 수 없습니다.
	 * 실제 보유 포인트 부족 여부는 Member.usePoint()에서 검증합니다.</p>
	 *
	 * @param totalAmount 총 주문 금액
	 * @param usedPointAmount 사용 포인트 금액
	 */
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

	/**
	 * 사용 포인트가 있으면 회원 포인트를 차감합니다.
	 *
	 * <p>포인트 잔액 부족 여부는 Member.usePoint()에서 검증합니다.</p>
	 *
	 * @param member 회원
	 * @param usedPointAmount 사용 포인트 금액
	 */
	private void usePointIfRequested(
		Member member,
		Long usedPointAmount
	) {
		if (usedPointAmount == 0) {
			return;
		}

		member.usePoint(usedPointAmount);
	}

	/**
	 * 상품 재고를 주문 수량만큼 차감합니다.
	 *
	 * <p>재고 차감은 Product 엔티티의 removeStock 메서드에 위임합니다.
	 * removeStock 내부에서 수량 검증, 재고 부족 검증, 재고 0 도달 시 상태 변경을 처리합니다.</p>
	 *
	 * @param cartItems 장바구니 상품 목록
	 * @param productMap 상품 ID를 key로 갖는 상품 Map
	 */
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

	/**
	 * 주문 정보를 저장합니다.
	 *
	 * @param member 회원
	 * @param totalAmount 총 주문 금액
	 * @param usedPointAmount 사용 포인트 금액
	 * @return 저장된 주문
	 */
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

	/**
	 * 주문 상품 정보를 저장합니다.
	 *
	 * @param order 주문
	 * @param cartItems 장바구니 상품 목록
	 * @param productMap 상품 ID를 key로 갖는 상품 Map
	 * @return 저장된 주문 상품 목록
	 */
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

	/**
	 * 결제 대기 정보를 생성합니다.
	 *
	 * @param memberId 회원 ID
	 * @param orderId 주문 ID
	 * @param totalAmount 총 주문 금액
	 * @param usedPointAmount 사용 포인트 금액
	 * @return 생성된 결제 정보
	 */
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

	/**
	 * 상품 Map에서 상품 ID에 해당하는 상품을 조회합니다.
	 *
	 * @param productMap 상품 ID를 key로 갖는 상품 Map
	 * @param productId 상품 ID
	 * @return 조회된 상품
	 */
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