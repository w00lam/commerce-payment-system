package com.commercepaymentsystem.domain.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.commercepaymentsystem.domain.cart.entity.CartItem;
import com.commercepaymentsystem.domain.cart.exception.CartErrorCode;
import com.commercepaymentsystem.domain.cart.service.CartItemCommand;
import com.commercepaymentsystem.domain.member.entity.Member;
import com.commercepaymentsystem.domain.member.service.MemberCommand;
import com.commercepaymentsystem.domain.order.dto.OrderPreviewRequest;
import com.commercepaymentsystem.domain.order.dto.OrderPreviewResponse;
import com.commercepaymentsystem.domain.product.entity.Product;
import com.commercepaymentsystem.domain.product.entity.ProductStatus;
import com.commercepaymentsystem.domain.product.exception.ProductErrorCode;
import com.commercepaymentsystem.domain.product.service.ProductCommand;
import com.commercepaymentsystem.global.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

	@Mock
	private MemberCommand memberCommand;

	@Mock
	private CartItemCommand cartItemCommand;

	@Mock
	private ProductCommand productCommand;

	@InjectMocks
	private OrderService orderService;

	@Test
	@DisplayName("장바구니 전체 주문서 미리보기 성공")
	void previewOrder_allCartItems_success() {
		// given
		Long memberId = 1L;

		Member member = mock(Member.class);
		when(member.getId()).thenReturn(memberId);

		Product macbook = mock(Product.class);
		when(macbook.getId()).thenReturn(1L);
		when(macbook.getName()).thenReturn("맥북 프로 16인치");
		when(macbook.getPrice()).thenReturn(3500000L);
		when(macbook.getStock()).thenReturn(10L);
		when(macbook.getStatus()).thenReturn(ProductStatus.ON_SALE);

		Product iphone = mock(Product.class);
		when(iphone.getId()).thenReturn(2L);
		when(iphone.getName()).thenReturn("아이폰 15 프로");
		when(iphone.getPrice()).thenReturn(1500000L);
		when(iphone.getStock()).thenReturn(50L);
		when(iphone.getStatus()).thenReturn(ProductStatus.ON_SALE);

		CartItem macbookCartItem = mock(CartItem.class);
		when(macbookCartItem.getId()).thenReturn(1L);
		when(macbookCartItem.getProductId()).thenReturn(1L);
		when(macbookCartItem.getQuantity()).thenReturn(1L);

		CartItem iphoneCartItem = mock(CartItem.class);
		when(iphoneCartItem.getId()).thenReturn(2L);
		when(iphoneCartItem.getProductId()).thenReturn(2L);
		when(iphoneCartItem.getQuantity()).thenReturn(2L);

		OrderPreviewRequest request = new OrderPreviewRequest(null);

		when(memberCommand.getMember(memberId))
			.thenReturn(member);

		when(cartItemCommand.getCartItemsByMemberId(memberId))
			.thenReturn(List.of(macbookCartItem, iphoneCartItem));

		when(productCommand.getProductsForOrder(List.of(1L, 2L)))
			.thenReturn(Map.of(
				1L, macbook,
				2L, iphone
			));

		// when
		OrderPreviewResponse response = orderService.previewOrder(memberId, request);

		// then
		assertThat(response.memberId()).isEqualTo(memberId);
		assertThat(response.totalAmount()).isEqualTo(6500000L);
		assertThat(response.items()).hasSize(2);

		assertThat(response.items().get(0).productName()).isEqualTo("맥북 프로 16인치");
		assertThat(response.items().get(0).currentPrice()).isEqualTo(3500000L);
		assertThat(response.items().get(0).quantity()).isEqualTo(1L);
		assertThat(response.items().get(0).totalPrice()).isEqualTo(3500000L);

		assertThat(response.items().get(1).productName()).isEqualTo("아이폰 15 프로");
		assertThat(response.items().get(1).currentPrice()).isEqualTo(1500000L);
		assertThat(response.items().get(1).quantity()).isEqualTo(2L);
		assertThat(response.items().get(1).totalPrice()).isEqualTo(3000000L);
	}

	@Test
	@DisplayName("선택한 장바구니 상품만 주문서 미리보기 성공")
	void previewOrder_selectedCartItems_success() {
		// given
		Long memberId = 1L;
		List<Long> cartItemIds = List.of(1L, 2L);

		Member member = mock(Member.class);
		when(member.getId()).thenReturn(memberId);

		Product macbook = mock(Product.class);
		when(macbook.getId()).thenReturn(1L);
		when(macbook.getName()).thenReturn("맥북 프로 16인치");
		when(macbook.getPrice()).thenReturn(3500000L);
		when(macbook.getStock()).thenReturn(10L);
		when(macbook.getStatus()).thenReturn(ProductStatus.ON_SALE);

		Product iphone = mock(Product.class);
		when(iphone.getId()).thenReturn(2L);
		when(iphone.getName()).thenReturn("아이폰 15 프로");
		when(iphone.getPrice()).thenReturn(1500000L);
		when(iphone.getStock()).thenReturn(50L);
		when(iphone.getStatus()).thenReturn(ProductStatus.ON_SALE);

		CartItem cartItem1 = mock(CartItem.class);
		when(cartItem1.getId()).thenReturn(1L);
		when(cartItem1.getProductId()).thenReturn(1L);
		when(cartItem1.getQuantity()).thenReturn(1L);

		CartItem cartItem2 = mock(CartItem.class);
		when(cartItem2.getId()).thenReturn(2L);
		when(cartItem2.getProductId()).thenReturn(2L);
		when(cartItem2.getQuantity()).thenReturn(2L);

		OrderPreviewRequest request = new OrderPreviewRequest(cartItemIds);

		when(memberCommand.getMember(memberId))
			.thenReturn(member);

		when(cartItemCommand.getCartItemsByMemberIdAndIds(memberId, cartItemIds))
			.thenReturn(List.of(cartItem1, cartItem2));

		when(productCommand.getProductsForOrder(List.of(1L, 2L)))
			.thenReturn(Map.of(
				1L, macbook,
				2L, iphone
			));

		// when
		OrderPreviewResponse response = orderService.previewOrder(memberId, request);

		// then
		assertThat(response.memberId()).isEqualTo(memberId);
		assertThat(response.totalAmount()).isEqualTo(6500000L);
		assertThat(response.items()).hasSize(2);
	}

	@Test
	@DisplayName("cartItemIds에 중복 ID가 포함되어도 중복 제거 후 주문서 미리보기에 성공한다")
	void previewOrder_duplicateCartItemIds_success() {
		// given
		Long memberId = 1L;
		List<Long> requestedCartItemIds = List.of(1L, 1L);
		List<Long> distinctCartItemIds = List.of(1L);

		Member member = mock(Member.class);
		when(member.getId()).thenReturn(memberId);

		Product macbook = mock(Product.class);
		when(macbook.getId()).thenReturn(1L);
		when(macbook.getName()).thenReturn("맥북 프로 16인치");
		when(macbook.getPrice()).thenReturn(3500000L);
		when(macbook.getStock()).thenReturn(10L);
		when(macbook.getStatus()).thenReturn(ProductStatus.ON_SALE);

		CartItem cartItem = mock(CartItem.class);
		when(cartItem.getId()).thenReturn(1L);
		when(cartItem.getProductId()).thenReturn(1L);
		when(cartItem.getQuantity()).thenReturn(1L);

		OrderPreviewRequest request = new OrderPreviewRequest(requestedCartItemIds);

		when(memberCommand.getMember(memberId))
			.thenReturn(member);

		when(cartItemCommand.getCartItemsByMemberIdAndIds(memberId, distinctCartItemIds))
			.thenReturn(List.of(cartItem));

		when(productCommand.getProductsForOrder(List.of(1L)))
			.thenReturn(Map.of(
				1L, macbook
			));

		// when
		OrderPreviewResponse response = orderService.previewOrder(memberId, request);

		// then
		assertThat(response.memberId()).isEqualTo(memberId);
		assertThat(response.totalAmount()).isEqualTo(3500000L);
		assertThat(response.items()).hasSize(1);

		verify(cartItemCommand).getCartItemsByMemberIdAndIds(
			memberId,
			distinctCartItemIds
		);
	}

	@Test
	@DisplayName("전체 장바구니 미리보기에서 장바구니가 비어 있으면 빈 응답을 반환한다")
	void previewOrder_emptyCart_success() {
		// given
		Long memberId = 1L;

		Member member = mock(Member.class);
		when(member.getId()).thenReturn(memberId);

		OrderPreviewRequest request = new OrderPreviewRequest(null);

		when(memberCommand.getMember(memberId))
			.thenReturn(member);

		when(cartItemCommand.getCartItemsByMemberId(memberId))
			.thenReturn(List.of());

		// when
		OrderPreviewResponse response = orderService.previewOrder(memberId, request);

		// then
		assertThat(response.memberId()).isEqualTo(memberId);
		assertThat(response.totalAmount()).isEqualTo(0L);
		assertThat(response.items()).isEmpty();
	}

	@Test
	@DisplayName("선택한 장바구니 상품 중 유효하지 않은 항목이 있으면 예외가 발생한다")
	void previewOrder_invalidCartItem_fail() {
		// given
		Long memberId = 1L;
		List<Long> cartItemIds = List.of(1L, 2L, 999L);

		Member member = mock(Member.class);
		when(member.getId()).thenReturn(memberId);

		CartItem cartItem = mock(CartItem.class);

		OrderPreviewRequest request = new OrderPreviewRequest(cartItemIds);

		when(memberCommand.getMember(memberId))
			.thenReturn(member);

		when(cartItemCommand.getCartItemsByMemberIdAndIds(memberId, cartItemIds))
			.thenReturn(List.of(cartItem));

		// when & then
		assertThatThrownBy(() -> orderService.previewOrder(memberId, request))
			.isInstanceOf(BusinessException.class)
			.hasMessage(CartErrorCode.CART_ITEM_NOT_FOUND.getMessage());
	}

	@Test
	@DisplayName("판매 중이 아닌 상품이면 예외가 발생한다")
	void previewOrder_notOnSaleProduct_fail() {
		// given
		Long memberId = 1L;

		Member member = mock(Member.class);
		when(member.getId()).thenReturn(memberId);

		Product product = mock(Product.class);
		when(product.getStatus()).thenReturn(ProductStatus.SOLD_OUT);

		CartItem cartItem = mock(CartItem.class);
		when(cartItem.getProductId()).thenReturn(1L);

		OrderPreviewRequest request = new OrderPreviewRequest(null);

		when(memberCommand.getMember(memberId))
			.thenReturn(member);

		when(cartItemCommand.getCartItemsByMemberId(memberId))
			.thenReturn(List.of(cartItem));

		when(productCommand.getProductsForOrder(List.of(1L)))
			.thenReturn(Map.of(
				1L, product
			));

		// when & then
		assertThatThrownBy(() -> orderService.previewOrder(memberId, request))
			.isInstanceOf(BusinessException.class)
			.hasMessage(ProductErrorCode.PRODUCT_NOT_ON_SALE.getMessage());
	}

	@Test
	@DisplayName("상품 재고가 부족하면 예외가 발생한다")
	void previewOrder_outOfStock_fail() {
		// given
		Long memberId = 1L;

		Member member = mock(Member.class);
		when(member.getId()).thenReturn(memberId);

		Product product = mock(Product.class);
		when(product.getStatus()).thenReturn(ProductStatus.ON_SALE);
		when(product.getStock()).thenReturn(1L);

		CartItem cartItem = mock(CartItem.class);
		when(cartItem.getProductId()).thenReturn(1L);
		when(cartItem.getQuantity()).thenReturn(2L);

		OrderPreviewRequest request = new OrderPreviewRequest(null);

		when(memberCommand.getMember(memberId))
			.thenReturn(member);

		when(cartItemCommand.getCartItemsByMemberId(memberId))
			.thenReturn(List.of(cartItem));

		when(productCommand.getProductsForOrder(List.of(1L)))
			.thenReturn(Map.of(
				1L, product
			));

		// when & then
		assertThatThrownBy(() -> orderService.previewOrder(memberId, request))
			.isInstanceOf(BusinessException.class)
			.hasMessage(ProductErrorCode.OUT_OF_STOCK.getMessage());
	}

	@Test
	@DisplayName("장바구니 상품의 productId에 해당하는 상품이 없으면 예외가 발생한다")
	void previewOrder_productNotFound_fail() {
		// given
		Long memberId = 1L;

		Member member = mock(Member.class);
		when(member.getId()).thenReturn(memberId);

		CartItem cartItem = mock(CartItem.class);
		when(cartItem.getProductId()).thenReturn(999L);

		OrderPreviewRequest request = new OrderPreviewRequest(null);

		when(memberCommand.getMember(memberId))
			.thenReturn(member);

		when(cartItemCommand.getCartItemsByMemberId(memberId))
			.thenReturn(List.of(cartItem));

		when(productCommand.getProductsForOrder(List.of(999L)))
			.thenThrow(new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

		// when & then
		assertThatThrownBy(() -> orderService.previewOrder(memberId, request))
			.isInstanceOf(BusinessException.class)
			.hasMessage(ProductErrorCode.PRODUCT_NOT_FOUND.getMessage());
	}
}