package com.commercepaymentsystem.domain.cart.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.commercepaymentsystem.domain.cart.entity.Cart;
import com.commercepaymentsystem.domain.cart.entity.CartItem;
import com.commercepaymentsystem.domain.cart.repository.CartItemRepository;
import com.commercepaymentsystem.domain.cart.repository.CartRepository;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

	@Mock
	private CartRepository cartRepository;

	@Mock
	private CartItemRepository cartItemRepository;

	@InjectMocks
	private CartService cartService;

	private Cart createCart(Long id, Long memberId) {
		Cart cart = Cart.create(memberId);
		ReflectionTestUtils.setField(cart, "id", id);
		return cart;
	}

	private CartItem createCartItem(Long id, Cart cart, Long productId, Long quantity) {
		CartItem cartItem = CartItem.create(cart, productId, quantity);
		ReflectionTestUtils.setField(cartItem, "id", id);
		return cartItem;
	}

	@Test
	@DisplayName("장바구니에 새 상품을 추가한다.")
	void addCartItem_NewProduct() {
		// given
		Long memberId = 1L;
		Long productId = 100L;
		Long quantity = 2L;

		Cart cart = createCart(10L, memberId);
		CartItem savedItem = createCartItem(1000L, cart, productId, quantity);

		when(cartRepository.findByMemberId(memberId)).thenReturn(Optional.of(cart));
		when(cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)).thenReturn(Optional.empty());
		when(cartItemRepository.save(any(CartItem.class))).thenReturn(savedItem);

		// when
		CartItem cartItem = cartService.addCartItem(memberId, productId, quantity);

		// then
		assertThat(cartItem.getId()).isEqualTo(savedItem.getId());
		assertThat(cartItem.getProductId()).isEqualTo(productId);
		assertThat(cartItem.getQuantity()).isEqualTo(quantity);
	}

	@Test
	@DisplayName("이미 담긴 상품을 다시 추가하면 수량을 합산한다.")
	void addCartItem_ExistingProduct() {
		// given
		Long memberId = 1L;
		Long productId = 100L;
		Long existingQuantity = 2L;
		Long addedQuantity = 3L;

		Cart cart = createCart(10L, memberId);
		CartItem existingItem = createCartItem(1000L, cart, productId, existingQuantity);

		when(cartRepository.findByMemberId(memberId)).thenReturn(Optional.of(cart));
		when(cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)).thenReturn(Optional.of(existingItem));

		// when
		CartItem cartItem = cartService.addCartItem(memberId, productId, addedQuantity);

		// then
		assertThat(cartItem.getQuantity()).isEqualTo(existingQuantity + addedQuantity);
	}

	@Test
	@DisplayName("장바구니 상품 수량을 변경한다.")
	void updateCartItemQuantity() {
		// given
		Cart cart = createCart(10L, 1L);
		CartItem cartItem = createCartItem(1000L, cart, 100L, 2L);

		// when
		CartItem updatedCartItem = cartService.updateCartItemQuantity(cartItem, 5L);

		// then
		assertThat(updatedCartItem.getQuantity()).isEqualTo(5L);
	}

	@Test
	@DisplayName("장바구니 상품을 삭제한다.")
	void deleteCartItem() {
		// given
		Long memberId = 1L;
		Long cartItemId = 1000L;
		Cart cart = createCart(10L, memberId);
		CartItem cartItem = createCartItem(cartItemId, cart, 100L, 2L);

		when(cartItemRepository.findByIdAndMemberId(cartItemId, memberId)).thenReturn(Optional.of(cartItem));

		// when
		Long deletedCartItemId = cartService.deleteCartItem(memberId, cartItemId);

		// then
		assertThat(deletedCartItemId).isEqualTo(cartItemId);
		verify(cartItemRepository).delete(cartItem);
	}

	@Test
	@DisplayName("장바구니 전체를 비운다.")
	void clearCart() {
		// given
		Long memberId = 1L;
		Cart cart = createCart(10L, memberId);
		CartItem cartItem1 = createCartItem(1000L, cart, 100L, 2L);
		CartItem cartItem2 = createCartItem(1001L, cart, 101L, 3L);

		when(cartRepository.findByMemberId(memberId)).thenReturn(Optional.of(cart));
		when(cartItemRepository.findAllByCartId(cart.getId())).thenReturn(List.of(cartItem1, cartItem2));

		// when
		Long cartId = cartService.clearCart(memberId);

		// then
		assertThat(cartId).isEqualTo(cart.getId());
		verify(cartItemRepository).deleteAllInBatch(anyList());
	}

	@Test
	@DisplayName("장바구니가 없으면 비우기 결과로 null을 반환한다.")
	void clearCart_EmptyCart() {
		// given
		Long memberId = 1L;

		when(cartRepository.findByMemberId(memberId)).thenReturn(Optional.empty());

		// when
		Long cartId = cartService.clearCart(memberId);

		// then
		assertThat(cartId).isNull();
	}
}
