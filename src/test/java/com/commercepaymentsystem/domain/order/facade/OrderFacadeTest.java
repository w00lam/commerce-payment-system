package com.commercepaymentsystem.domain.order.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import com.commercepaymentsystem.domain.cart.entity.Cart;
import com.commercepaymentsystem.domain.cart.entity.CartItem;
import com.commercepaymentsystem.domain.cart.exception.CartErrorCode;
import com.commercepaymentsystem.domain.cart.service.CartService;
import com.commercepaymentsystem.domain.member.entity.Member;
import com.commercepaymentsystem.domain.member.service.MemberService;
import com.commercepaymentsystem.domain.order.dto.OrderCancelResponse;
import com.commercepaymentsystem.domain.order.dto.OrderCreateRequest;
import com.commercepaymentsystem.domain.order.dto.OrderCreateResponse;
import com.commercepaymentsystem.domain.order.dto.OrderPreviewRequest;
import com.commercepaymentsystem.domain.order.dto.OrderPreviewResponse;
import com.commercepaymentsystem.domain.order.entity.Order;
import com.commercepaymentsystem.domain.order.entity.OrderItem;
import com.commercepaymentsystem.domain.order.entity.OrderStatus;
import com.commercepaymentsystem.domain.order.exception.OrderErrorCode;
import com.commercepaymentsystem.domain.order.service.OrderNumberGenerator;
import com.commercepaymentsystem.domain.order.service.OrderService;
import com.commercepaymentsystem.domain.payment.dto.PaymentCreateCommand;
import com.commercepaymentsystem.domain.payment.dto.PaymentCreateResult;
import com.commercepaymentsystem.domain.payment.dto.PaymentConfirmCommand;
import com.commercepaymentsystem.domain.payment.dto.PaymentConfirmResult;
import com.commercepaymentsystem.domain.payment.entity.Payment;
import com.commercepaymentsystem.domain.payment.entity.PaymentStatus;
import com.commercepaymentsystem.domain.payment.exception.PaymentErrorCode;
import com.commercepaymentsystem.domain.payment.exception.PaymentException;
import com.commercepaymentsystem.domain.payment.facade.PaymentConfirmFacade;
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
	private OrderNumberGenerator orderNumberGenerator;

	@Mock
	private PaymentConfirmFacade paymentConfirmFacade;

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

	private OrderItem createOrderItem(
		Long id,
		Product product,
		Long quantity
	) {
		OrderItem orderItem = new OrderItem(
			product,
			product.getPrice(),
			quantity
		);

		ReflectionTestUtils.setField(orderItem, "id", id);

		return orderItem;
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

	private Payment createPendingPayment(
		Long id,
		String paymentId,
		Long memberId,
		Long orderId,
		Long totalOrderAmount,
		Long usedPointAmount,
		Long finalPaymentAmount
	) {
		Payment payment = Payment.create(
			paymentId,
			memberId,
			orderId,
			totalOrderAmount,
			usedPointAmount,
			finalPaymentAmount
		);

		ReflectionTestUtils.setField(payment, "id", id);

		return payment;
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

		assertThat(response.items().get(0).productId()).isEqualTo(firstProductId);
		assertThat(response.items().get(0).productName()).isEqualTo("키보드");
		assertThat(response.items().get(0).price()).isEqualTo(30000L);
		assertThat(response.items().get(0).quantity()).isEqualTo(2L);
		assertThat(response.items().get(0).subtotal()).isEqualTo(60000L);

		assertThat(response.items().get(1).productId()).isEqualTo(secondProductId);
		assertThat(response.items().get(1).productName()).isEqualTo("마우스");
		assertThat(response.items().get(1).price()).isEqualTo(20000L);
		assertThat(response.items().get(1).quantity()).isEqualTo(1L);
		assertThat(response.items().get(1).subtotal()).isEqualTo(20000L);

		verify(cartService).findCartEntitiesByIds(memberId, List.of(1L, 2L));
		verify(productService).getProduct(firstProductId);
		verify(productService).getProduct(secondProductId);
	}

	@Test
	@DisplayName("주문서 미리보기에서 장바구니가 비어 있으면 빈 응답을 반환한다.")
	void previewOrder_EmptyCart_ReturnsEmptyResponse() {
		// given
		Long memberId = 1L;
		OrderPreviewRequest request = new OrderPreviewRequest(List.of());

		when(cartService.findCartEntities(memberId))
			.thenReturn(List.of());

		// when
		OrderPreviewResponse response = orderFacade.previewOrder(memberId, request);

		// then
		assertThat(response.memberId()).isEqualTo(memberId);
		assertThat(response.totalAmount()).isZero();
		assertThat(response.items()).isEmpty();

		verify(cartService).findCartEntities(memberId);
		verify(productService, never()).getProduct(any());
	}

	@Test
	@DisplayName("주문서 미리보기 시 판매 중이 아닌 상품이 있으면 예외가 발생한다.")
	void previewOrder_ProductNotOnSale_ThrowsException() {
		// given
		Long memberId = 1L;
		Long productId = 100L;

		OrderPreviewRequest request = new OrderPreviewRequest(List.of(1L));
		CartItem cartItem = createCartItem(1L, memberId, productId, 1L);

		Product product = createProduct(
			productId,
			"키보드",
			30000L,
			10L,
			ProductStatus.SOLD_OUT
		);

		when(cartService.findCartEntitiesByIds(memberId, List.of(1L)))
			.thenReturn(List.of(cartItem));
		when(productService.getProduct(productId))
			.thenReturn(product);

		// when & then
		assertThatThrownBy(() -> orderFacade.previewOrder(memberId, request))
			.isInstanceOf(BusinessException.class)
			.hasMessage(ProductErrorCode.PRODUCT_NOT_ON_SALE.getMessage());

		verify(cartService).findCartEntitiesByIds(memberId, List.of(1L));
		verify(productService).getProduct(productId);
	}

	@Test
	@DisplayName("주문 생성 시 선택한 장바구니 상품으로 주문과 결제를 생성한다.")
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

		OrderItem firstOrderItem = new OrderItem(
			firstProduct,
			firstProduct.getPrice(),
			firstCartItem.getQuantity()
		);

		OrderItem secondOrderItem = new OrderItem(
			secondProduct,
			secondProduct.getPrice(),
			secondCartItem.getQuantity()
		);

		Order savedOrder = createSavedOrder(
			member,
			List.of(firstOrderItem, secondOrderItem),
			80000L,
			usedPointAmount
		);

		when(memberService.getMember(memberId))
			.thenReturn(member);
		when(cartService.findCartEntitiesByIds(memberId, List.of(1L, 2L)))
			.thenReturn(List.of(firstCartItem, secondCartItem));
		when(productService.deductProductStocks(List.of(firstCartItem, secondCartItem)))
			.thenReturn(List.of(firstProduct, secondProduct));
		when(orderService.createOrder(
			eq(member),
			eq(List.of(firstCartItem, secondCartItem)),
			eq(List.of(firstProduct, secondProduct)),
			eq(usedPointAmount)
		))
			.thenReturn(savedOrder);
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
		assertThat(response.paymentOrderName()).isEqualTo(savedOrder.getOrderName());
		assertThat(response.memberId()).isEqualTo(memberId);
		assertThat(response.totalAmount()).isEqualTo(80000L);
		assertThat(response.usedPointAmount()).isEqualTo(usedPointAmount);
		assertThat(response.finalPaymentAmount()).isEqualTo(79000L);
		assertThat(response.orderStatus()).isEqualTo(OrderStatus.CREATED);
		assertThat(response.paymentId()).isEqualTo("PAY-20260603-000001");
		assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.PENDING);
		assertThat(response.items()).hasSize(2);

		assertThat(response.items().get(0).productId()).isEqualTo(firstProductId);
		assertThat(response.items().get(0).productName()).isEqualTo("키보드");
		assertThat(response.items().get(0).orderPrice()).isEqualTo(30000L);
		assertThat(response.items().get(0).quantity()).isEqualTo(2L);
		assertThat(response.items().get(0).totalPrice()).isEqualTo(60000L);

		assertThat(response.items().get(1).productId()).isEqualTo(secondProductId);
		assertThat(response.items().get(1).productName()).isEqualTo("마우스");
		assertThat(response.items().get(1).orderPrice()).isEqualTo(20000L);
		assertThat(response.items().get(1).quantity()).isEqualTo(1L);
		assertThat(response.items().get(1).totalPrice()).isEqualTo(20000L);

		assertThat(member.getPointBalance()).isEqualTo(5000L);

		ArgumentCaptor<PaymentCreateCommand> commandCaptor =
			ArgumentCaptor.forClass(PaymentCreateCommand.class);

		verify(paymentService).createPendingPayment(commandCaptor.capture());

		PaymentCreateCommand command = commandCaptor.getValue();

		assertThat(command.memberId()).isEqualTo(memberId);
		assertThat(command.orderId()).isEqualTo(1000L);
		assertThat(command.totalOrderAmount()).isEqualTo(80000L);
		assertThat(command.usedPointAmount()).isEqualTo(usedPointAmount);
		assertThat(command.finalPaymentAmount()).isEqualTo(79000L);
		assertThat(command.orderName()).isEqualTo(savedOrder.getOrderName());

		verify(memberService).getMember(memberId);
		verify(cartService).findCartEntitiesByIds(memberId, List.of(1L, 2L));
		verify(productService).deductProductStocks(List.of(firstCartItem, secondCartItem));
		verify(orderService).createOrder(
			eq(member),
			eq(List.of(firstCartItem, secondCartItem)),
			eq(List.of(firstProduct, secondProduct)),
			eq(usedPointAmount)
		);

		verify(productService, never()).getProduct(firstProductId);
		verify(productService, never()).getProduct(secondProductId);
		verify(cartService, never()).clearCartItems(anyList(), any());
		verify(paymentConfirmFacade, never()).confirm(any());
	}

	@Test
	@DisplayName("Point-only order creation confirms payment automatically")
	void createOrder_PointOnlyPayment_AutoConfirm() {
		Long memberId = 1L;
		Long productId = 100L;
		Long usedPointAmount = 30000L;
		String paymentId = "PAY-point-only";

		OrderCreateRequest request = new OrderCreateRequest(
			List.of(1L),
			usedPointAmount
		);

		Member member = createMember(memberId, 30000L);
		CartItem cartItem = createCartItem(1L, memberId, productId, 1L);
		Product product = createProduct(
			productId,
			"Keyboard",
			30000L,
			10L,
			ProductStatus.ON_SALE
		);
		OrderItem orderItem = new OrderItem(
			product,
			product.getPrice(),
			cartItem.getQuantity()
		);
		Order savedOrder = createSavedOrder(
			member,
			List.of(orderItem),
			30000L,
			usedPointAmount
		);

		when(memberService.getMember(memberId))
			.thenReturn(member);
		when(cartService.findCartEntitiesByIds(memberId, List.of(1L)))
			.thenReturn(List.of(cartItem));
		when(productService.deductProductStocks(List.of(cartItem)))
			.thenReturn(List.of(product));
		when(orderService.createOrder(
			eq(member),
			eq(List.of(cartItem)),
			eq(List.of(product)),
			eq(usedPointAmount)
		))
			.thenReturn(savedOrder);
		when(paymentService.createPendingPayment(any(PaymentCreateCommand.class)))
			.thenReturn(new PaymentCreateResult(
				paymentId,
				memberId,
				1000L,
				30000L,
				usedPointAmount,
				0L,
				PaymentStatus.PENDING
			));
		when(paymentConfirmFacade.confirm(PaymentConfirmCommand.of(paymentId, memberId)))
			.thenReturn(new PaymentConfirmResult(
				paymentId,
				memberId,
				1000L,
				0L,
				PaymentStatus.CONFIRMED,
				Instant.parse("2026-06-01T01:02:03Z")
			));
		OrderCreateResponse response = orderFacade.createOrder(memberId, request);

		assertThat(response.paymentId()).isEqualTo(paymentId);
		assertThat(response.finalPaymentAmount()).isZero();
		assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.CONFIRMED);
		verify(paymentConfirmFacade).confirm(PaymentConfirmCommand.of(paymentId, memberId));
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
			.hasMessage(OrderErrorCode.EMPTY_ORDER_ITEM.getMessage());

		verify(memberService).getMember(memberId);
		verify(cartService).findCartEntities(memberId);
		verify(productService, never()).deductProductStocks(anyList());
		verify(orderService, never()).createOrder(any(), anyList(), anyList(), any());
		verify(paymentService, never()).createPendingPayment(any());
		verify(cartService, never()).clearCartItems(anyList(), any());
	}

	@Test
	@DisplayName("주문 생성 시 선택한 장바구니 상품 일부를 찾지 못하면 예외가 발생한다.")
	void createOrder_SelectedCartItemNotFound_ThrowsException() {
		// given
		Long memberId = 1L;
		Long productId = 100L;

		OrderCreateRequest request = new OrderCreateRequest(List.of(1L, 2L), 1000L);
		Member member = createMember(memberId, 5000L);
		CartItem cartItem = createCartItem(1L, memberId, productId, 1L);

		when(memberService.getMember(memberId))
			.thenReturn(member);
		when(cartService.findCartEntitiesByIds(memberId, List.of(1L, 2L)))
			.thenReturn(List.of(cartItem));

		// when & then
		assertThatThrownBy(() -> orderFacade.createOrder(memberId, request))
			.isInstanceOf(BusinessException.class)
			.hasMessage(CartErrorCode.CART_ITEM_NOT_FOUND.getMessage());

		verify(memberService).getMember(memberId);
		verify(cartService).findCartEntitiesByIds(memberId, List.of(1L, 2L));
		verify(productService, never()).deductProductStocks(anyList());
		verify(orderService, never()).createOrder(any(), anyList(), anyList(), any());
		verify(paymentService, never()).createPendingPayment(any());
	}

	@Test
	@DisplayName("주문 생성 시 판매 중이 아닌 상품이 있으면 주문과 결제를 생성하지 않는다.")
	void createOrder_ProductNotOnSale_ThrowsException() {
		// given
		Long memberId = 1L;
		Long productId = 100L;

		OrderCreateRequest request = new OrderCreateRequest(List.of(1L), 1000L);
		Member member = createMember(memberId, 5000L);
		CartItem cartItem = createCartItem(1L, memberId, productId, 1L);

		when(memberService.getMember(memberId))
			.thenReturn(member);
		when(cartService.findCartEntitiesByIds(memberId, List.of(1L)))
			.thenReturn(List.of(cartItem));
		when(productService.deductProductStocks(List.of(cartItem)))
			.thenThrow(new BusinessException(ProductErrorCode.PRODUCT_NOT_ON_SALE));

		// when & then
		assertThatThrownBy(() -> orderFacade.createOrder(memberId, request))
			.isInstanceOf(BusinessException.class)
			.hasMessage(ProductErrorCode.PRODUCT_NOT_ON_SALE.getMessage());

		assertThat(member.getPointBalance()).isEqualTo(5000L);

		verify(memberService).getMember(memberId);
		verify(cartService).findCartEntitiesByIds(memberId, List.of(1L));
		verify(productService).deductProductStocks(List.of(cartItem));
		verify(orderService, never()).createOrder(any(), anyList(), anyList(), any());
		verify(paymentService, never()).createPendingPayment(any());
		verify(cartService, never()).clearCartItems(anyList(), any());
	}

	@Test
	@DisplayName("주문 생성 시 사용 포인트가 회원 보유 포인트보다 크면 예외가 발생한다.")
	void createOrder_UsedPointExceedsMemberPointBalance_ThrowsException() {
		// given
		Long memberId = 1L;
		Long usedPointAmount = 6000L;

		OrderCreateRequest request = new OrderCreateRequest(
			List.of(1L),
			usedPointAmount
		);

		Member member = createMember(memberId, 5000L);

		when(memberService.getMember(memberId))
			.thenReturn(member);

		// when & then
		assertThatThrownBy(() -> orderFacade.createOrder(memberId, request))
			.isInstanceOf(BusinessException.class)
			.hasMessage("포인트 잔액이 부족합니다.");

		verify(memberService).getMember(memberId);
		verify(cartService, never()).findCartEntities(any());
		verify(cartService, never()).findCartEntitiesByIds(any(), anyList());
		verify(productService, never()).deductProductStocks(anyList());
		verify(orderService, never()).createOrder(any(), anyList(), anyList(), any());
		verify(paymentService, never()).createPendingPayment(any());
		verify(cartService, never()).clearCartItems(anyList(), any());
	}

	@Test
	@DisplayName("주문 생성 시 사용 포인트가 주문 총액보다 크면 예외가 발생한다.")
	void createOrder_UsedPointExceedsTotalPrice_ThrowsException() {
		// given
		Long memberId = 1L;
		Long productId = 100L;
		Long usedPointAmount = 40000L;

		OrderCreateRequest request = new OrderCreateRequest(
			List.of(1L),
			usedPointAmount
		);

		Member member = createMember(memberId, 50000L);
		CartItem cartItem = createCartItem(1L, memberId, productId, 1L);

		Product product = createProduct(
			productId,
			"키보드",
			30000L,
			10L,
			ProductStatus.ON_SALE
		);

		when(memberService.getMember(memberId))
			.thenReturn(member);
		when(cartService.findCartEntitiesByIds(memberId, List.of(1L)))
			.thenReturn(List.of(cartItem));
		when(productService.deductProductStocks(List.of(cartItem)))
			.thenReturn(List.of(product));
		when(orderService.createOrder(
			eq(member),
			eq(List.of(cartItem)),
			eq(List.of(product)),
			eq(usedPointAmount)
		))
			.thenThrow(new BusinessException(OrderErrorCode.INVALID_POINT_AMOUNT));

		// when & then
		assertThatThrownBy(() -> orderFacade.createOrder(memberId, request))
			.isInstanceOf(BusinessException.class)
			.hasMessage(OrderErrorCode.INVALID_POINT_AMOUNT.getMessage());

		verify(memberService).getMember(memberId);
		verify(cartService).findCartEntitiesByIds(memberId, List.of(1L));
		verify(productService).deductProductStocks(List.of(cartItem));
		verify(orderService).createOrder(
			eq(member),
			eq(List.of(cartItem)),
			eq(List.of(product)),
			eq(usedPointAmount)
		);
		verify(paymentService, never()).createPendingPayment(any());
		verify(cartService, never()).clearCartItems(anyList(), any());
	}

	@Test
	@DisplayName("주문 취소 시 주문과 결제를 취소 처리하고 상품 재고를 복구한다.")
	void cancelOrder_PendingPayment_Success() {
		// given
		Long memberId = 1L;
		Long orderId = 1000L;

		Product product = mock(Product.class);
		when(product.getId()).thenReturn(10L);

		OrderItem orderItem = mock(OrderItem.class);
		when(orderItem.getProduct()).thenReturn(product);
		when(orderItem.getQuantity()).thenReturn(2L);

		Order order = mock(Order.class);
		when(order.getId()).thenReturn(orderId);
		when(order.getOrderItems()).thenReturn(List.of(orderItem));

		Payment payment = mock(Payment.class);

		when(orderService.getMyOrderDetailForUpdate(orderId, memberId))
			.thenReturn(order);

		when(paymentService.getPendingPaymentByOrderIdForUpdate(order.getId(), memberId))
			.thenReturn(payment);

		// when
		OrderCancelResponse response = orderFacade.cancelOrder(memberId, orderId);

		// then
		InOrder inOrder = inOrder(paymentService, orderService);
		inOrder.verify(paymentService).getPendingPaymentByOrderIdForUpdate(orderId, memberId);
		inOrder.verify(orderService).getMyOrderDetailForUpdate(orderId, memberId);

		verify(orderService).cancelOrder(order);
		verify(paymentService).failPayment(payment);

		ArgumentCaptor<Map<Long, Long>> productQuantitiesCaptor =
			ArgumentCaptor.forClass(Map.class);

		verify(productService).restoreProductStocks(productQuantitiesCaptor.capture());

		Map<Long, Long> productQuantities = productQuantitiesCaptor.getValue();

		assertThat(productQuantities)
			.containsEntry(product.getId(), orderItem.getQuantity());

		assertThat(response).isNotNull();
	}

	@Test
	@DisplayName("주문 취소 시 주문을 찾지 못하면 결제 조회와 재고 복구를 수행하지 않는다.")
	void cancelOrder_OrderNotFound_ThrowsException() {
		// given
		Long memberId = 1L;
		Long orderId = 1000L;
		Payment payment = mock(Payment.class);

		when(paymentService.getPendingPaymentByOrderIdForUpdate(orderId, memberId))
			.thenReturn(payment);
		lenient().when(orderService.getMyOrderDetailForUpdate(orderId, memberId))
			.thenThrow(new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));

		// when & then
		assertThatThrownBy(() -> orderFacade.cancelOrder(memberId, orderId))
			.isInstanceOf(BusinessException.class)
			.hasMessage(OrderErrorCode.ORDER_NOT_FOUND.getMessage());

		verify(paymentService).getPendingPaymentByOrderIdForUpdate(orderId, memberId);
		verify(orderService).getMyOrderDetailForUpdate(orderId, memberId);
		verify(orderService, never()).cancelOrder(any());
		verify(paymentService, never()).failPayment(any());
		verify(orderService, never()).restoreProductStock(any(), any());
	}

	@Test
	@DisplayName("주문 취소 시 결제가 대기 상태가 아니면 주문 취소와 재고 복구를 수행하지 않는다.")
	@MockitoSettings(strictness = Strictness.LENIENT)
	void cancelOrder_PaymentNotPending_ThrowsException() {
		// given
		Long memberId = 1L;
		Long orderId = 1000L;

		Member member = createMember(memberId, 5000L);

		Product product = createProduct(
			100L,
			"키보드",
			30000L,
			10L,
			ProductStatus.ON_SALE
		);

		OrderItem orderItem = createOrderItem(
			10L,
			product,
			2L
		);

		Order order = createSavedOrder(
			member,
			List.of(orderItem),
			60000L,
			1000L
		);

		when(orderService.getMyOrderDetailForUpdate(orderId, memberId))
			.thenReturn(order);
		when(paymentService.getPendingPaymentByOrderIdForUpdate(orderId, memberId))
			.thenThrow(new PaymentException(PaymentErrorCode.INVALID_PAYMENT_STATUS));

		// when & then
		assertThatThrownBy(() -> orderFacade.cancelOrder(memberId, orderId))
			.isInstanceOf(PaymentException.class)
			.hasMessage(PaymentErrorCode.INVALID_PAYMENT_STATUS.getMessage());

		verify(paymentService).getPendingPaymentByOrderIdForUpdate(orderId, memberId);
		verify(orderService, never()).getMyOrderDetailForUpdate(any(), any());
		verify(orderService, never()).cancelOrder(any());
		verify(paymentService, never()).failPayment(any());
		verify(orderService, never()).restoreProductStock(any(), any());
	}

	@Test
	@DisplayName("주문 취소 시 주문 상태 전이가 불가능하면 결제 실패 처리와 재고 복구를 수행하지 않는다.")
	void cancelOrder_InvalidOrderStatus_ThrowsException() {
		// given
		Long memberId = 1L;
		Long orderId = 1000L;

		Member member = createMember(memberId, 5000L);

		Product product = createProduct(
			100L,
			"키보드",
			30000L,
			10L,
			ProductStatus.ON_SALE
		);

		OrderItem orderItem = createOrderItem(
			10L,
			product,
			2L
		);

		Order order = createSavedOrder(
			member,
			List.of(orderItem),
			60000L,
			1000L
		);

		Payment payment = createPendingPayment(
			100L,
			"PAY-20260604-000001",
			memberId,
			orderId,
			60000L,
			1000L,
			59000L
		);

		when(orderService.getMyOrderDetailForUpdate(orderId, memberId))
			.thenReturn(order);
		when(paymentService.getPendingPaymentByOrderIdForUpdate(orderId, memberId))
			.thenReturn(payment);
		doThrow(new BusinessException(OrderErrorCode.INVALID_ORDER_STATUS))
			.when(orderService)
			.cancelOrder(order);

		// when & then
		assertThatThrownBy(() -> orderFacade.cancelOrder(memberId, orderId))
			.isInstanceOf(BusinessException.class)
			.hasMessage(OrderErrorCode.INVALID_ORDER_STATUS.getMessage());

		verify(orderService).getMyOrderDetailForUpdate(orderId, memberId);
		verify(paymentService).getPendingPaymentByOrderIdForUpdate(orderId, memberId);
		verify(orderService).cancelOrder(order);
		verify(paymentService, never()).failPayment(any());
		verify(orderService, never()).restoreProductStock(any(), any());
	}
}
