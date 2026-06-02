package com.commercepaymentsystem.domain.order.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commercepaymentsystem.domain.cart.entity.CartItem;
import com.commercepaymentsystem.domain.cart.exception.CartErrorCode;
import com.commercepaymentsystem.domain.cart.service.CartService;
import com.commercepaymentsystem.domain.member.entity.Member;
import com.commercepaymentsystem.domain.member.service.MemberService;
import com.commercepaymentsystem.domain.order.dto.OrderPreviewItemResponse;
import com.commercepaymentsystem.domain.order.dto.OrderPreviewRequest;
import com.commercepaymentsystem.domain.order.dto.OrderPreviewResponse;
import com.commercepaymentsystem.domain.order.mapper.OrderPreviewMapper;
import com.commercepaymentsystem.domain.product.entity.Product;
import com.commercepaymentsystem.domain.product.entity.ProductStatus;
import com.commercepaymentsystem.domain.product.exception.ProductErrorCode;
import com.commercepaymentsystem.domain.product.service.ProductService;
import com.commercepaymentsystem.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

	private final MemberService memberService;
	private final CartService cartService;
	private final ProductService productService;

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
		Member member = memberService.getMember(memberId);

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
	 * 주문서 미리보기에 사용할 장바구니 상품 목록을 조회합니다.
	 *
	 * <p>cartItemIds가 비어 있으면 회원의 전체 장바구니 상품을 조회하고,
	 * 값이 있으면 중복을 제거한 뒤 해당 ID 목록에 포함된 장바구니 상품만 조회합니다.
	 * 선택 상품 미리보기 요청에서 존재하지 않는 장바구니 상품 ID가 포함되면 예외를 발생시킵니다.</p>
	 *
	 * @param memberId 검증된 회원 ID
	 * @param cartItemIds 장바구니 상품 ID 목록
	 * @return 주문서 미리보기에 사용할 장바구니 상품 목록
	 */
	private List<CartItem> findPreviewCartItems(
		Long memberId,
		List<Long> cartItemIds
	) {
		if (cartItemIds == null || cartItemIds.isEmpty()) {
			return cartService.getCartItemsByMemberId(memberId);
		}

		List<Long> distinctCartItemIds = cartItemIds.stream()
			.distinct()
			.toList();

		List<CartItem> cartItems = cartService.getCartItemsByMemberIdAndIds(
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

		return productService.getRequiredProductMap(productIds);
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
	 * 상품이 주문 가능한 상태인지 검증합니다.
	 *
	 * <p>판매 중인 상품인지 확인하고, 장바구니 수량보다 재고가 충분한지 검증합니다.
	 * 이 메서드는 재고를 차감하지 않고, 주문 가능 여부만 확인합니다.</p>
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
