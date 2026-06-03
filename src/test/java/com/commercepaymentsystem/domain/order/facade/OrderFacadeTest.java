package com.commercepaymentsystem.domain.order.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.commercepaymentsystem.domain.cart.entity.Cart;
import com.commercepaymentsystem.domain.cart.entity.CartItem;
import com.commercepaymentsystem.domain.cart.exception.CartErrorCode;
import com.commercepaymentsystem.domain.cart.service.CartService;
import com.commercepaymentsystem.domain.member.entity.Member;
import com.commercepaymentsystem.domain.member.service.MemberService;
import com.commercepaymentsystem.domain.order.dto.OrderCreateRequest;
import com.commercepaymentsystem.domain.order.dto.OrderCreateResponse;
import com.commercepaymentsystem.domain.order.dto.OrderPreviewRequest;
import com.commercepaymentsystem.domain.order.dto.OrderPreviewResponse;
import com.commercepaymentsystem.domain.order.entity.Order;
import com.commercepaymentsystem.domain.order.entity.OrderItem;
import com.commercepaymentsystem.domain.order.entity.OrderStatus;
import com.commercepaymentsystem.domain.order.service.OrderFacade;
import com.commercepaymentsystem.domain.order.service.OrderService;
import com.commercepaymentsystem.domain.payment.dto.PaymentCreateCommand;
import com.commercepaymentsystem.domain.payment.dto.PaymentCreateResult;
import com.commercepaymentsystem.domain.payment.entity.PaymentStatus;
import com.commercepaymentsystem.domain.payment.service.PaymentService;
import com.commercepaymentsystem.domain.product.entity.Product;
import com.commercepaymentsystem.domain.product.entity.ProductCategory;
import com.commercepaymentsystem.domain.product.entity.ProductStatus;
import com.commercepaymentsystem.domain.product.exception.ProductErrorCode;
import com.commercepaymentsystem.domain.product.service.ProductService;
import com.commercepaymentsystem.global.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class OrderFacadeTest {

	@Mock
	private CartService cartService;

	@Mock
	private MemberService memberService;

	@Mock
	private OrderService orderService;

	@Mock
	private PaymentService paymentService;

	@Mock
	private ProductService productService;

	@InjectMocks
	private OrderFacade orderFacade;

	private Member createMember(Long id, Long pointBalance) {
		Member member = Member.create(
			"user@example.com",
			"encoded-password",
			"홍길동",
			"010-1234-5678"
		);

		ReflectionTestUtils.setField(member, "id", id);
		ReflectionTestUtils.setField(member, "pointBalance", pointBalance);

		return member;
	}

	private Product createProduct(
		Long id,
		String name,
		Long price,
		Long stock,
		ProductStatus status
	) {
		Product product = Product.create(
			name,
			price,
			stock,
			"desc",
			status,
			ProductCategory.ELECTRONICS
		);

		ReflectionTestUtils.setField(product, "id", id);

		return product;
	}

	private CartItem createCartItem(
		Long id,
		Long memberId,
		Long productId,
		Long quantity
	) {
		Cart cart = Cart.create(memberId);
		ReflectionTestUtils.setField(cart, "id", 10L);

		CartItem cartItem = CartItem.create(cart, productId, quantity);
		ReflectionTestUtils.setField(cartItem, "id", id);

		return cartItem;
	}

	private Order createSavedOrder(
		Member member,
		List<OrderItem> orderItems,
		Long totalPrice,
		Long usedPointAmount
	) {
		Order order = new Order(
			member,
			totalPrice,
			orderItems,
			usedPointAmount,
			"ORD-20260603-000001"
		);

		ReflectionTestUtils.setField(order, "id", 1000L);

		return order;
	}

	@Test
	@DisplayName("주문서 미리보기 시 선택한 장바구니 상품만 조회하고 총액을 계산한다.")
	void previewOrder_SelectedCartItems_Success() {
		// given
		Long memberId = 1L;
		Long firstProductId = 100L;
		Long secondProductId = 200L;

		OrderPreviewRequest request = new OrderPreviewRequest(List.of(1L, 2L, 1L));

		CartItem firstCartItem = createCartItem(1L, memberId, firstProductId, 2L);
		CartItem secondCartItem = createCartItem(2L, memberId, secondProductId, 1L);

		Product firstProduct = createProduct(
			firstProductId,
			"키보드",
			30000L,
			10L,
			ProductStatus.ON_SALE
		);

		Product secondProduct = createProduct(
			secondProductId,
			"마우스",
			20000L,
			5L,
			ProductStatus.ON_SALE
		);

		when(cartService.findCartEntitiesByIds(memberId, List.of(1L, 2L)))
			.thenReturn(List.of(firstCartItem, secondCartItem));
		when(productService.getProduct(firstProductId))
			.thenReturn(firstProduct);
		when(productService.getProduct(secondProductId))
			.thenReturn(secondProduct);

		// when
		OrderPreviewResponse response = orderFacade.previewOrder(memberId, request);

		// then
		assertThat(response.memberId()).isEqualTo(memberId);
		assertThat(response.totalAmount()).isEqualTo(80000L);
		assertThat(response.items()).hasSize(2);

		assertThat(response.items().get(0).productName()).isEqualTo("키보드");
		assertThat(response.items().get(0).subtotal()).isEqualTo(60000L);

		assertThat(response.items().get(1).productName()).isEqualTo("마우스");
		assertThat(response.items().get(1).subtotal()).isEqualTo(20000L);

		verify(cartService).findCartEntitiesByIds(memberId, List.of(1L, 2L));
	}

	@Test
	@DisplayName("주문서 미리보기에서 선택한 장바구니 상품 일부가 없으면 예외가 발생한다.")
	void previewOrder_SelectedCartItemNotFound_ThrowsException() {
		// given
		Long memberId = 1L;
		OrderPreviewRequest request = new OrderPreviewRequest(List.of(1L, 2L));
		CartItem cartItem = createCartItem(1L, memberId, 100L, 1L);

		when(cartService.findCartEntitiesByIds(memberId, List.of(1L, 2L)))
			.thenReturn(List.of(cartItem));

		// when & then
		assertThatThrownBy(() -> orderFacade.previewOrder(memberId, request))
			.isInstanceOf(BusinessException.class)
			.hasMessage(CartErrorCode.CART_ITEM_NOT_FOUND.getMessage());

		verify(productService, never()).getProduct(any());
	}

	@Test
	@DisplayName("주문 생성 시 선택한 장바구니 상품으로 주문과 결제를 생성하고 선택 상품만 삭제한다.")
	void createOrder_SelectedCartItems_Success() {
		// given
		Long memberId = 1L;
		Long firstProductId = 100L;
		Long secondProductId = 200L;
		Long usedPointAmount = 1000L;

		OrderCreateRequest request = new OrderCreateRequest(
			List.of(1L, 2L, 1L),
			usedPointAmount
		);

		Member member = createMember(memberId, 5000L);

		CartItem firstCartItem = createCartItem(1L, memberId, firstProductId, 2L);
		CartItem secondCartItem = createCartItem(2L, memberId, secondProductId, 1L);

		Product firstProduct = createProduct(
			firstProductId,
			"키보드",
			30000L,
			10L,
			ProductStatus.ON_SALE
		);

		Product secondProduct = createProduct(
			secondProductId,
			"마우스",
			20000L,
			5L,
			ProductStatus.ON_SALE
		);

		when(memberService.getMember(memberId))
			.thenReturn(member);
		when(cartService.findCartEntitiesByIds(memberId, List.of(1L, 2L)))
			.thenReturn(List.of(firstCartItem, secondCartItem));
		when(productService.getProduct(firstProductId))
			.thenReturn(firstProduct);
		when(productService.getProduct(secondProductId))
			.thenReturn(secondProduct);
		when(orderService.createOrder(eq(member), anyList(), eq(80000L), eq(usedPointAmount)))
			.thenAnswer(invocation -> {
				List<OrderItem> orderItems = invocation.getArgument(1);
				return createSavedOrder(
					member,
					orderItems,
					80000L,
					usedPointAmount
				);
			});
		when(paymentService.createPendingPayment(any(PaymentCreateCommand.class)))
			.thenReturn(new PaymentCreateResult(
				"PAY-20260603-000001",
				memberId,
				1000L,
				80000L,
				usedPointAmount,
				79000L,
				PaymentStatus.PENDING
			));

		// when
		OrderCreateResponse response = orderFacade.createOrder(memberId, request);

		// then
		assertThat(response.orderId()).isEqualTo(1000L);
		assertThat(response.orderNumber()).isEqualTo("ORD-20260603-000001");
		assertThat(response.memberId()).isEqualTo(memberId);
		assertThat(response.totalAmount()).isEqualTo(80000L);
		assertThat(response.usedPointAmount()).isEqualTo(usedPointAmount);
		assertThat(response.finalPaymentAmount()).isEqualTo(79000L);
		assertThat(response.orderStatus()).isEqualTo(OrderStatus.CREATED);
		assertThat(response.paymentId()).isEqualTo("PAY-20260603-000001");
		assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.PENDING);
		assertThat(response.items()).hasSize(2);

		assertThat(firstProduct.getStock()).isEqualTo(8L);
		assertThat(secondProduct.getStock()).isEqualTo(4L);
		assertThat(member.getPointBalance()).isEqualTo(4000L);

		ArgumentCaptor<PaymentCreateCommand> commandCaptor =
			ArgumentCaptor.forClass(PaymentCreateCommand.class);

		verify(paymentService).createPendingPayment(commandCaptor.capture());

		PaymentCreateCommand command = commandCaptor.getValue();

		assertThat(command.memberId()).isEqualTo(memberId);
		assertThat(command.orderId()).isEqualTo(1000L);
		assertThat(command.totalOrderAmount()).isEqualTo(80000L);
		assertThat(command.usedPointAmount()).isEqualTo(usedPointAmount);
		assertThat(command.finalPaymentAmount()).isEqualTo(79000L);

		verify(cartService).clearCartItems(List.of(1L, 2L), memberId);
	}

	@Test
	@DisplayName("주문 생성 시 장바구니가 비어 있으면 주문과 결제를 생성하지 않는다.")
	void createOrder_EmptyCart_ThrowsException() {
		// given
		Long memberId = 1L;
		OrderCreateRequest request = new OrderCreateRequest(List.of(), 1000L);
		Member member = createMember(memberId, 5000L);

		when(memberService.getMember(memberId))
			.thenReturn(member);
		when(cartService.findCartEntities(memberId))
			.thenReturn(List.of());

		// when & then
		assertThatThrownBy(() -> orderFacade.createOrder(memberId, request))
			.isInstanceOf(BusinessException.class)
			.hasMessage(CartErrorCode.CART_ITEM_NOT_FOUND.getMessage());

		verify(productService, never()).getProduct(any());
		verify(orderService, never()).createOrder(any(), anyList(), any(), any());
		verify(paymentService, never()).createPendingPayment(any());
		verify(cartService, never()).clearCartItems(anyList(), any());
	}

	@Test
	@DisplayName("주문 생성 시 판매 중이 아닌 상품이 있으면 재고 차감과 주문 생성을 하지 않는다.")
	void createOrder_ProductNotOnSale_ThrowsException() {
		// given
		Long memberId = 1L;
		Long productId = 100L;

		OrderCreateRequest request = new OrderCreateRequest(List.of(1L), 1000L);
		Member member = createMember(memberId, 5000L);
		CartItem cartItem = createCartItem(1L, memberId, productId, 1L);

		Product product = createProduct(
			productId,
			"키보드",
			30000L,
			10L,
			ProductStatus.SOLD_OUT
		);

		when(memberService.getMember(memberId))
			.thenReturn(member);
		when(cartService.findCartEntitiesByIds(memberId, List.of(1L)))
			.thenReturn(List.of(cartItem));
		when(productService.getProduct(productId))
			.thenReturn(product);

		// when & then
		assertThatThrownBy(() -> orderFacade.createOrder(memberId, request))
			.isInstanceOf(BusinessException.class)
			.hasMessage(ProductErrorCode.PRODUCT_NOT_ON_SALE.getMessage());

		assertThat(product.getStock()).isEqualTo(10L);
		assertThat(member.getPointBalance()).isEqualTo(5000L);

		verify(orderService, never()).createOrder(any(), anyList(), any(), any());
		verify(paymentService, never()).createPendingPayment(any());
		verify(cartService, never()).clearCartItems(anyList(), any());
	}
}