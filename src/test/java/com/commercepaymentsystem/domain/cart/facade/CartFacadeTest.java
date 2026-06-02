package com.commercepaymentsystem.domain.cart.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.commercepaymentsystem.domain.cart.dto.CartItemAddRequest;
import com.commercepaymentsystem.domain.cart.dto.CartItemAddResponse;
import com.commercepaymentsystem.domain.cart.dto.CartItemQuantityUpdateRequest;
import com.commercepaymentsystem.domain.cart.dto.CartItemUpdateResponse;
import com.commercepaymentsystem.domain.cart.dto.CartResponse;
import com.commercepaymentsystem.domain.cart.entity.Cart;
import com.commercepaymentsystem.domain.cart.entity.CartItem;
import com.commercepaymentsystem.domain.cart.exception.CartErrorCode;
import com.commercepaymentsystem.domain.cart.service.CartService;
import com.commercepaymentsystem.domain.member.entity.Member;
import com.commercepaymentsystem.domain.member.service.MemberService;
import com.commercepaymentsystem.domain.product.entity.Product;
import com.commercepaymentsystem.domain.product.entity.ProductCategory;
import com.commercepaymentsystem.domain.product.entity.ProductStatus;
import com.commercepaymentsystem.domain.product.service.ProductService;
import com.commercepaymentsystem.global.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class CartFacadeTest {

	@Mock
	private CartService cartService;

	@Mock
	private MemberService memberService;

	@Mock
	private ProductService productService;

	@InjectMocks
	private CartFacade cartFacade;

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
	@DisplayName("상품 재고를 검증한 뒤 장바구니에 상품을 추가한다.")
	void addCartItem_success() {
		// given
		Long memberId = 1L;
		Long productId = 100L;
		Long quantity = 2L;
		Cart cart = createCart(10L, memberId);
		Product product = createProduct(productId, "Test Product", 1000L, 10L);
		CartItem cartItem = createCartItem(1000L, cart, productId, quantity);
		CartItemAddRequest request = new CartItemAddRequest(productId, quantity);

		when(memberService.getMember(memberId)).thenReturn(org.mockito.Mockito.mock(Member.class));
		when(productService.getProduct(productId)).thenReturn(product);
		when(cartService.getCartItemQuantity(memberId, productId)).thenReturn(0L);
		when(cartService.addCartItem(memberId, productId, quantity)).thenReturn(cartItem);

		// when
		CartItemAddResponse response = cartFacade.addCartItem(memberId, request);

		// then
		assertThat(response.cartId()).isEqualTo(cart.getId());
		assertThat(response.productId()).isEqualTo(productId);
		assertThat(response.quantity()).isEqualTo(quantity);
		verify(memberService).getMember(memberId);
	}

	@Test
	@DisplayName("합산 수량이 재고를 초과하면 장바구니에 추가하지 않는다.")
	void addCartItem_OutOfStock() {
		// given
		Long memberId = 1L;
		Long productId = 100L;
		CartItemAddRequest request = new CartItemAddRequest(productId, 6L);
		Product product = createProduct(productId, "Test Product", 1000L, 10L);

		when(memberService.getMember(memberId)).thenReturn(org.mockito.Mockito.mock(Member.class));
		when(productService.getProduct(productId)).thenReturn(product);
		when(cartService.getCartItemQuantity(memberId, productId)).thenReturn(5L);

		// when & then
		assertThatThrownBy(() -> cartFacade.addCartItem(memberId, request))
			.isInstanceOf(BusinessException.class)
			.hasMessage(CartErrorCode.OUT_OF_STOCK.getMessage());

		verify(cartService, never()).addCartItem(memberId, productId, request.quantity());
	}

	@Test
	@DisplayName("장바구니 조회 응답을 상품 정보와 조합한다.")
	void getMyCart_ExistingCart() {
		// given
		Long memberId = 1L;
		Cart cart = createCart(10L, memberId);
		CartItem cartItem = createCartItem(1000L, cart, 100L, 2L);
		Product product = createProduct(100L, "Test Product", 1000L, 10L);

		when(memberService.getMember(memberId)).thenReturn(org.mockito.Mockito.mock(Member.class));
		when(cartService.getCart(memberId)).thenReturn(Optional.of(cart));
		when(cartService.getCartItems(cart.getId())).thenReturn(List.of(cartItem));
		when(productService.getProductMap(List.of(100L))).thenReturn(Map.of(100L, product));

		// when
		CartResponse response = cartFacade.getMyCart(memberId);

		// then
		assertThat(response.cartId()).isEqualTo(cart.getId());
		assertThat(response.items()).hasSize(1);
		assertThat(response.items().get(0).productName()).isEqualTo("Test Product");
		assertThat(response.items().get(0).quantity()).isEqualTo(2L);
		assertThat(response.totalAmount()).isEqualTo(2000L);
	}

	@Test
	@DisplayName("장바구니 조회 시 상품이 누락된 항목은 기존 정책대로 제외한다.")
	void getMyCart_SkipMissingProduct() {
		// given
		Long memberId = 1L;
		Cart cart = createCart(10L, memberId);
		CartItem cartItem = createCartItem(1000L, cart, 100L, 2L);

		when(memberService.getMember(memberId)).thenReturn(org.mockito.Mockito.mock(Member.class));
		when(cartService.getCart(memberId)).thenReturn(Optional.of(cart));
		when(cartService.getCartItems(cart.getId())).thenReturn(List.of(cartItem));
		when(productService.getProductMap(List.of(100L))).thenReturn(Map.of());

		// when
		CartResponse response = cartFacade.getMyCart(memberId);

		// then
		assertThat(response.items()).isEmpty();
		assertThat(response.totalAmount()).isEqualTo(0L);
	}

	@Test
	@DisplayName("수량 변경 전 상품 재고를 검증한다.")
	void updateCartItemQuantity_success() {
		// given
		Long memberId = 1L;
		Long cartItemId = 1000L;
		Long productId = 100L;
		Long newQuantity = 5L;
		Cart cart = createCart(10L, memberId);
		CartItem cartItem = createCartItem(cartItemId, cart, productId, 2L);
		CartItem updatedCartItem = createCartItem(cartItemId, cart, productId, newQuantity);
		Product product = createProduct(productId, "Test Product", 1000L, 10L);
		CartItemQuantityUpdateRequest request = new CartItemQuantityUpdateRequest(newQuantity);

		when(memberService.getMember(memberId)).thenReturn(org.mockito.Mockito.mock(Member.class));
		when(cartService.getCartItem(memberId, cartItemId)).thenReturn(cartItem);
		when(productService.getProduct(productId)).thenReturn(product);
		when(cartService.updateCartItemQuantity(cartItem, newQuantity)).thenReturn(updatedCartItem);

		// when
		CartItemUpdateResponse response = cartFacade.updateCartItemQuantity(memberId, cartItemId, request);

		// then
		assertThat(response.cartItemId()).isEqualTo(cartItemId);
		assertThat(response.quantity()).isEqualTo(newQuantity);
	}
}
