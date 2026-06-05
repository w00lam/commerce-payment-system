package com.commercepaymentsystem.domain.cart.facade;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commercepaymentsystem.domain.cart.dto.CartClearResponse;
import com.commercepaymentsystem.domain.cart.dto.CartItemAddRequest;
import com.commercepaymentsystem.domain.cart.dto.CartItemAddResponse;
import com.commercepaymentsystem.domain.cart.dto.CartItemDeleteResponse;
import com.commercepaymentsystem.domain.cart.dto.CartItemDto;
import com.commercepaymentsystem.domain.cart.dto.CartItemQuantityUpdateRequest;
import com.commercepaymentsystem.domain.cart.dto.CartItemUpdateResponse;
import com.commercepaymentsystem.domain.cart.dto.CartResponse;
import com.commercepaymentsystem.domain.cart.entity.Cart;
import com.commercepaymentsystem.domain.cart.entity.CartItem;
import com.commercepaymentsystem.domain.cart.exception.CartErrorCode;
import com.commercepaymentsystem.domain.cart.service.CartService;
import com.commercepaymentsystem.domain.member.service.MemberService;
import com.commercepaymentsystem.domain.product.entity.Product;
import com.commercepaymentsystem.domain.product.service.ProductService;
import com.commercepaymentsystem.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartFacade {

	private final CartService cartService;
	private final MemberService memberService;
	private final ProductService productService;

	@Transactional
	public CartItemAddResponse addCartItem(Long memberId, CartItemAddRequest request) {
		memberService.getMember(memberId);

		Product product = productService.getProduct(request.productId());
		Long currentQuantity = cartService.getCartItemQuantity(memberId, product.getId());

		validateEnoughStock(product, currentQuantity + request.quantity());

		CartItem cartItem = cartService.addCartItem(
			memberId,
			product.getId(),
			request.quantity()
		);

		return CartItemAddResponse.from(cartItem);
	}

	public CartResponse getMyCart(Long memberId) {
		memberService.getMember(memberId);

		return cartService.getCart(memberId)
			.map(cart -> toCartResponse(memberId, cart))
			.orElseGet(() -> CartResponse.of(null, memberId, List.of(), 0L));
	}

	@Transactional
	public CartItemUpdateResponse updateCartItemQuantity(
		Long memberId,
		Long cartItemId,
		CartItemQuantityUpdateRequest request
	) {
		memberService.getMember(memberId);

		CartItem cartItem = cartService.getCartItem(memberId, cartItemId);
		Product product = productService.getProduct(cartItem.getProductId());

		validateEnoughStock(product, request.quantity());

		CartItem updatedCartItem = cartService.updateCartItemQuantity(
			cartItem,
			request.quantity()
		);

		return CartItemUpdateResponse.of(updatedCartItem.getId(), updatedCartItem.getQuantity());
	}

	@Transactional
	public CartItemDeleteResponse deleteCartItem(Long memberId, Long cartItemId) {
		memberService.getMember(memberId);

		Long deletedCartItemId = cartService.deleteCartItem(memberId, cartItemId);
		return CartItemDeleteResponse.of(deletedCartItemId);
	}

	@Transactional
	public CartClearResponse clearCart(Long memberId) {
		memberService.getMember(memberId);

		Long cartId = cartService.clearCart(memberId);
		return CartClearResponse.of(cartId);
	}

	private CartResponse toCartResponse(Long memberId, Cart cart) {
		List<CartItem> cartItems = cartService.getCartItems(cart.getId());
		List<Long> productIds = cartItems.stream()
			.map(CartItem::getProductId)
			.toList();
		Map<Long, Product> productMap = productService.getProductMap(productIds);

		List<CartItemDto> itemDtos = cartItems.stream()
			.map(cartItem -> toCartItemDto(cartItem, productMap))
			.filter(Objects::nonNull)
			.toList();

		Long totalAmount = itemDtos.stream()
			.mapToLong(dto -> dto.price() * dto.quantity())
			.sum();

		return CartResponse.of(cart.getId(), memberId, itemDtos, totalAmount);
	}

	private CartItemDto toCartItemDto(CartItem cartItem, Map<Long, Product> productMap) {
		Product product = productMap.get(cartItem.getProductId());

		if (product == null) {
			return null;
		}

		return CartItemDto.of(cartItem, product);
	}

	private void validateEnoughStock(Product product, Long quantity) {
		if (product.getStock() < quantity) {
			throw new BusinessException(CartErrorCode.OUT_OF_STOCK);
		}
	}
}
