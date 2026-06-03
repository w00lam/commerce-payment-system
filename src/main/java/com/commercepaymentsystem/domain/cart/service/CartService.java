package com.commercepaymentsystem.domain.cart.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commercepaymentsystem.domain.cart.entity.Cart;
import com.commercepaymentsystem.domain.cart.entity.CartItem;
import com.commercepaymentsystem.domain.cart.exception.CartErrorCode;
import com.commercepaymentsystem.domain.cart.repository.CartItemRepository;
import com.commercepaymentsystem.domain.cart.repository.CartRepository;
import com.commercepaymentsystem.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

	private final CartRepository cartRepository;
	private final CartItemRepository cartItemRepository;

	@Transactional
	public CartItem addCartItem(
		Long memberId,
		Long productId,
		Long quantity
	) {
		Cart cart = cartRepository.findByMemberId(memberId)
			.orElseGet(() -> cartRepository.save(Cart.create(memberId)));

		CartItem existingCartItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
			.orElse(null);

		if (existingCartItem != null) {
			existingCartItem.addQuantity(quantity);
			return existingCartItem;
		}

		return cartItemRepository.save(CartItem.create(cart, productId, quantity));
	}

	public Optional<Cart> getCart(Long memberId) {
		return cartRepository.findByMemberId(memberId);
	}

	public List<CartItem> getCartItems(Long cartId) {
		return cartItemRepository.findAllByCartId(cartId);
	}

	public Long getCartItemQuantity(Long memberId, Long productId) {
		return cartRepository.findByMemberId(memberId)
			.flatMap(cart -> cartItemRepository.findByCartIdAndProductId(cart.getId(), productId))
			.map(CartItem::getQuantity)
			.orElse(0L);
	}

	public CartItem getCartItem(Long memberId, Long cartItemId) {
		return cartItemRepository.findByIdAndMemberId(cartItemId, memberId)
			.orElseThrow(() -> new BusinessException(CartErrorCode.CART_ITEM_NOT_FOUND));
	}

	@Transactional
	public CartItem updateCartItemQuantity(CartItem cartItem, Long quantity) {
		cartItem.updateQuantity(quantity);
		return cartItem;
	}

	@Transactional
	public Long deleteCartItem(Long memberId, Long cartItemId) {
		CartItem cartItem = getCartItem(memberId, cartItemId);

		cartItemRepository.delete(cartItem);
		return cartItem.getId();
	}

	@Transactional
	public Long clearCart(Long memberId) {
		return cartRepository.findByMemberId(memberId)
			.map(cart -> {
				cartItemRepository.deleteAllInBatch(cartItemRepository.findAllByCartId(cart.getId()));
				return cart.getId();
			})
			.orElse(null);
	}

	public List<CartItem> findCartEntities(Long memberId) {
		return cartItemRepository.findAllByMemberId(memberId);
	}

	public List<CartItem> findCartEntitiesByIds(Long memberId, List<Long> cartItemIds) {
		return cartItemRepository.findByIdInAndCartMemberIdWithCart(cartItemIds, memberId);
	}

	public void clearCartItems(List<Long> orderedItemIds, Long memberId) {
		int deleted = cartItemRepository.deleteAllByIdInAndMemberId(orderedItemIds, memberId);
		if (deleted != orderedItemIds.size()) {
			throw new BusinessException(CartErrorCode.CART_ITEM_NOT_FOUND);
		}
	}
}
