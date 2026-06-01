package com.commercepaymentsystem.domain.order.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commercepaymentsystem.domain.cart.entity.CartItem;
import com.commercepaymentsystem.domain.cart.exception.CartErrorCode;
import com.commercepaymentsystem.domain.cart.repository.CartItemRepository;
import com.commercepaymentsystem.domain.member.entity.Member;
import com.commercepaymentsystem.domain.member.exception.MemberErrorCode;
import com.commercepaymentsystem.domain.member.repository.MemberRepository;
import com.commercepaymentsystem.domain.order.dto.OrderPreviewItemResponse;
import com.commercepaymentsystem.domain.order.dto.OrderPreviewRequest;
import com.commercepaymentsystem.domain.order.dto.OrderPreviewResponse;
import com.commercepaymentsystem.domain.order.mapper.OrderPreviewMapper;
import com.commercepaymentsystem.domain.product.entity.Product;
import com.commercepaymentsystem.domain.product.exception.ProductErrorCode;
import com.commercepaymentsystem.domain.product.repository.ProductRepository;
import com.commercepaymentsystem.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

	private final MemberRepository memberRepository;
	private final CartItemRepository cartItemRepository;
	private final ProductRepository productRepository;

	/**
	 * 주문서 미리보기 정보를 조회합니다.
	 *
	 * <p>장바구니 상품을 기준으로 주문 예정 상품 목록과 총 주문 금액을 계산합니다.
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
		Member member = findMember(memberId);

		List<CartItem> cartItems = findPreviewCartItems(
			memberId,
			request.cartItemIds()
		);

		validateCartItems(cartItems);

		Map<Long, Product> productMap = findProductMap(cartItems);

		validateEnoughStock(
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
	 * 회원 ID로 회원을 조회합니다.
	 *
	 * @param memberId 회원 ID
	 * @return 조회된 회원
	 */
	private Member findMember(Long memberId) {
		return memberRepository.findById(memberId)
			.orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
	}

	/**
	 * 주문서 미리보기에 사용할 장바구니 상품 목록을 조회합니다.
	 *
	 * <p>cartItemIds가 비어 있으면 회원의 전체 장바구니 상품을 조회하고,
	 * 값이 있으면 해당 ID 목록에 포함된 장바구니 상품만 조회합니다.</p>
	 *
	 * @param memberId 회원 ID
	 * @param cartItemIds 장바구니 상품 ID 목록
	 * @return 주문서 미리보기에 사용할 장바구니 상품 목록
	 */
	private List<CartItem> findPreviewCartItems(
		Long memberId,
		List<Long> cartItemIds
	) {
		if (cartItemIds == null || cartItemIds.isEmpty()) {
			return cartItemRepository.findAllByMemberId(memberId);
		}

		List<CartItem> cartItems = cartItemRepository.findAllByMemberIdAndIdIn(
			memberId,
			cartItemIds
		);

		if (cartItems.size() != cartItemIds.size()) {
			throw new BusinessException(CartErrorCode.CART_ITEM_NOT_FOUND);
		}

		return cartItems;
	}

	/**
	 * 장바구니 상품 목록이 비어 있는지 검증합니다.
	 *
	 * @param cartItems 장바구니 상품 목록
	 */
	private void validateCartItems(List<CartItem> cartItems) {
		if (cartItems.isEmpty()) {
			throw new BusinessException(CartErrorCode.CART_ITEM_NOT_FOUND);
		}
	}

	/**
	 * 장바구니 상품에 연결된 상품 목록을 조회하고 Map으로 변환합니다.
	 *
	 * @param cartItems 장바구니 상품 목록
	 * @return 상품 ID를 key로 갖는 상품 Map
	 */
	private Map<Long, Product> findProductMap(List<CartItem> cartItems) {
		List<Long> productIds = cartItems.stream()
			.map(CartItem::getProductId)
			.distinct()
			.toList();

		List<Product> products = productRepository.findAllById(productIds);

		if (products.size() != productIds.size()) {
			throw new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND);
		}

		return products.stream()
			.collect(Collectors.toMap(
				Product::getId,
				Function.identity()
			));
	}

	/**
	 * 상품 재고가 장바구니 수량보다 충분한지 검증합니다.
	 *
	 * <p>이 메서드는 재고를 차감하지 않고, 부족 여부만 확인합니다.</p>
	 *
	 * @param cartItems 장바구니 상품 목록
	 * @param productMap 상품 ID를 key로 갖는 상품 Map
	 */
	private void validateEnoughStock(
		List<CartItem> cartItems,
		Map<Long, Product> productMap
	) {
		for (CartItem cartItem : cartItems) {
			Product product = getProduct(
				productMap,
				cartItem.getProductId()
			);

			if (product.getStock() < cartItem.getQuantity()) {
				throw new BusinessException(ProductErrorCode.OUT_OF_STOCK);
			}
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