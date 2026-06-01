package com.commercepaymentsystem.domain.cart.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.commercepaymentsystem.domain.cart.dto.CartClearResponse;
import com.commercepaymentsystem.domain.cart.dto.CartItemAddRequest;
import com.commercepaymentsystem.domain.cart.dto.CartItemAddResponse;
import com.commercepaymentsystem.domain.cart.dto.CartItemDeleteResponse;
import com.commercepaymentsystem.domain.cart.dto.CartItemQuantityUpdateRequest;
import com.commercepaymentsystem.domain.cart.dto.CartItemUpdateResponse;
import com.commercepaymentsystem.domain.cart.dto.CartResponse;
import com.commercepaymentsystem.domain.cart.entity.Cart;
import com.commercepaymentsystem.domain.cart.entity.CartItem;
import com.commercepaymentsystem.domain.cart.exception.CartErrorCode;
import com.commercepaymentsystem.domain.cart.repository.CartItemRepository;
import com.commercepaymentsystem.domain.cart.repository.CartRepository;
import com.commercepaymentsystem.domain.product.entity.Product;
import com.commercepaymentsystem.domain.product.entity.ProductCategory;
import com.commercepaymentsystem.domain.product.entity.ProductStatus;
import com.commercepaymentsystem.domain.product.exception.ProductErrorCode;
import com.commercepaymentsystem.domain.product.repository.ProductRepository;
import com.commercepaymentsystem.global.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CartService cartService;

    private Product createProduct(Long id, String name, Long price, Long stock) {
        Product product = Product.create(name, price, stock, "desc", ProductStatus.ON_SALE, ProductCategory.ELECTRONICS);
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

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
    @DisplayName("장바구니에 새로운 상품을 추가한다.")
    void addCartItem_NewProduct() {
        // given
        Long memberId = 1L;
        Long productId = 100L;
        Long quantity = 2L;
        CartItemAddRequest request = new CartItemAddRequest(productId, quantity);

        Product product = createProduct(productId, "Test Product", 1000L, 10L);
        Cart cart = createCart(10L, memberId);
        CartItem savedItem = createCartItem(1000L, cart, productId, quantity);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(cartRepository.findByMemberId(memberId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(savedItem);

        // when
        CartItemAddResponse response = cartService.addCartItem(memberId, request);

        // then
        assertThat(response.cartId()).isEqualTo(cart.getId());
        assertThat(response.productId()).isEqualTo(productId);
        assertThat(response.quantity()).isEqualTo(quantity);
    }

    @Test
    @DisplayName("장바구니에 이미 담긴 상품을 추가하면 수량이 합산된다.")
    void addCartItem_ExistingProduct() {
        // given
        Long memberId = 1L;
        Long productId = 100L;
        Long existingQuantity = 2L;
        Long addedQuantity = 3L;
        CartItemAddRequest request = new CartItemAddRequest(productId, addedQuantity);

        Product product = createProduct(productId, "Test Product", 1000L, 10L);
        Cart cart = createCart(10L, memberId);
        CartItem existingItem = createCartItem(1000L, cart, productId, existingQuantity);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(cartRepository.findByMemberId(memberId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)).thenReturn(Optional.of(existingItem));

        // when
        CartItemAddResponse response = cartService.addCartItem(memberId, request);

        // then
        assertThat(response.quantity()).isEqualTo(existingQuantity + addedQuantity);
    }

    @Test
    @DisplayName("장바구니 추가 시 수량이 재고를 초과하면 예외가 발생한다.")
    void addCartItem_OutOfStock() {
        // given
        Long memberId = 1L;
        Long productId = 100L;
        Long quantity = 11L; // Stock is 10
        CartItemAddRequest request = new CartItemAddRequest(productId, quantity);

        Product product = createProduct(productId, "Test Product", 1000L, 10L);
        Cart cart = createCart(10L, memberId);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(cartRepository.findByMemberId(memberId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> cartService.addCartItem(memberId, request))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining(CartErrorCode.OUT_OF_STOCK.getMessage());
    }

    @Test
    @DisplayName("장바구니에 담긴 상품 수량을 변경한다.")
    void updateCartItemQuantity() {
        // given
        Long memberId = 1L;
        Long cartItemId = 1000L;
        Long productId = 100L;
        Long newQuantity = 5L;
        CartItemQuantityUpdateRequest request = new CartItemQuantityUpdateRequest(newQuantity);

        Cart cart = createCart(10L, memberId);
        CartItem cartItem = createCartItem(cartItemId, cart, productId, 2L);
        Product product = createProduct(productId, "Test Product", 1000L, 10L);

        when(cartItemRepository.findByIdAndMemberId(cartItemId, memberId)).thenReturn(Optional.of(cartItem));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        // when
        CartItemUpdateResponse response = cartService.updateCartItemQuantity(memberId, cartItemId, request);

        // then
        assertThat(response.quantity()).isEqualTo(newQuantity);
    }

    @Test
    @DisplayName("장바구니 상품 단건을 삭제한다.")
    void deleteCartItem() {
        // given
        Long memberId = 1L;
        Long cartItemId = 1000L;
        Cart cart = createCart(10L, memberId);
        CartItem cartItem = createCartItem(cartItemId, cart, 100L, 2L);

        when(cartItemRepository.findByIdAndMemberId(cartItemId, memberId)).thenReturn(Optional.of(cartItem));

        // when
        CartItemDeleteResponse response = cartService.deleteCartItem(memberId, cartItemId);

        // then
        assertThat(response.cartItemId()).isEqualTo(cartItemId);
        assertThat(cartItem.getDeletedAt()).isNotNull();
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
        CartClearResponse response = cartService.clearCart(memberId);

        // then
        assertThat(response.cartId()).isEqualTo(cart.getId());
        assertThat(cartItem1.getDeletedAt()).isNotNull();
        assertThat(cartItem2.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("기존 장바구니 목록을 정상적으로 조회한다.")
    void getMyCart_ExistingCart() {
        // given
        Long memberId = 1L;
        Cart cart = createCart(10L, memberId);
        CartItem cartItem = createCartItem(1000L, cart, 100L, 2L);
        Product product = createProduct(100L, "Test Product", 1000L, 10L);

        when(cartRepository.findByMemberId(memberId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findAllByCartId(cart.getId())).thenReturn(List.of(cartItem));
        when(productRepository.findAllById(List.of(100L))).thenReturn(List.of(product));

        // when
        CartResponse response = cartService.getMyCart(memberId);

        // then
        assertThat(response.cartId()).isEqualTo(cart.getId());
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).productId()).isEqualTo(100L);
        assertThat(response.items().get(0).productName()).isEqualTo("Test Product");
        assertThat(response.items().get(0).quantity()).isEqualTo(2L);
    }

    @Test
    @DisplayName("장바구니 조회 시, 장바구니가 없으면 빈 결과를 반환한다.")
    void getMyCart_NewCart() {
        // given
        Long memberId = 1L;

        when(cartRepository.findByMemberId(memberId)).thenReturn(Optional.empty());

        // when
        CartResponse response = cartService.getMyCart(memberId);

        // then
        assertThat(response.cartId()).isNull();
        assertThat(response.items()).isEmpty();
        verify(cartRepository, never()).save(any(Cart.class));
    }
}
