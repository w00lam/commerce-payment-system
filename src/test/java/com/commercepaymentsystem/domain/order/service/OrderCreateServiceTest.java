package com.commercepaymentsystem.domain.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.commercepaymentsystem.domain.cart.entity.CartItem;
import com.commercepaymentsystem.domain.cart.exception.CartErrorCode;
import com.commercepaymentsystem.domain.cart.service.CartItemCommand;
import com.commercepaymentsystem.domain.cart.service.CartService;
import com.commercepaymentsystem.domain.member.entity.Member;
import com.commercepaymentsystem.domain.member.service.MemberCommand;
import com.commercepaymentsystem.domain.order.dto.OrderCreateRequest;
import com.commercepaymentsystem.domain.order.dto.OrderCreateResponse;
import com.commercepaymentsystem.domain.order.entity.Order;
import com.commercepaymentsystem.domain.order.entity.OrderItem;
import com.commercepaymentsystem.domain.order.exception.OrderErrorCode;
import com.commercepaymentsystem.domain.order.repository.OrderItemRepository;
import com.commercepaymentsystem.domain.order.repository.OrderRepository;
import com.commercepaymentsystem.domain.payment.dto.PaymentCreateCommand;
import com.commercepaymentsystem.domain.payment.dto.PaymentCreateResult;
import com.commercepaymentsystem.domain.payment.entity.PaymentStatus;
import com.commercepaymentsystem.domain.payment.service.PaymentService;
import com.commercepaymentsystem.domain.point.exception.PointErrorCode;
import com.commercepaymentsystem.domain.product.entity.Product;
import com.commercepaymentsystem.domain.product.entity.ProductStatus;
import com.commercepaymentsystem.domain.product.exception.ProductErrorCode;
import com.commercepaymentsystem.domain.product.service.ProductCommand;
import com.commercepaymentsystem.global.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class OrderCreateServiceTest {

	@Mock
	private MemberCommand memberCommand;

	@Mock
	private CartItemCommand cartItemCommand;

	@Mock
	private ProductCommand productCommand;

	@Mock
	private CartService cartService;

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private OrderItemRepository orderItemRepository;

	@Mock
	private OrderNumberGenerator orderNumberGenerator;

	@Mock
	private PaymentService paymentService;

	@InjectMocks
	private OrderService orderService;

	@Test
	@DisplayName("장바구니 상품 기준 주문 생성에 성공한다")
	void createOrder_success() {
		// given
		Long memberId = 1L;
		Long cartItemId = 1L;
		Long productId = 1L;

		Member member = mock(Member.class);
		when(member.getId()).thenReturn(memberId);

		CartItem cartItem = mock(CartItem.class);
		when(cartItem.getProductId()).thenReturn(productId);
		when(cartItem.getQuantity()).thenReturn(2L);

		Product product = mock(Product.class);
		when(product.getId()).thenReturn(productId);
		when(product.getName()).thenReturn("맥북 프로 16인치");
		when(product.getPrice()).thenReturn(3500000L);
		when(product.getStock()).thenReturn(10L);
		when(product.getStatus()).thenReturn(ProductStatus.ON_SALE);

		OrderCreateRequest request = new OrderCreateRequest(
			List.of(cartItemId),
			1000L
		);

		Order savedOrder = Order.create(
			member,
			"ORD-20260602-000001",
			7000000L,
			1000L
		);

		OrderItem savedOrderItem = mock(OrderItem.class);
		when(savedOrderItem.getId()).thenReturn(1L);
		when(savedOrderItem.getProductId()).thenReturn(productId);
		when(savedOrderItem.getProductName()).thenReturn("맥북 프로 16인치");
		when(savedOrderItem.getOrderPrice()).thenReturn(3500000L);
		when(savedOrderItem.getQuantity()).thenReturn(2L);
		when(savedOrderItem.getTotalPrice()).thenReturn(7000000L);

		PaymentCreateResult payment = new PaymentCreateResult(
			"PAY-20260602-000001",
			memberId,
			1L,
			7000000L,
			1000L,
			6999000L,
			PaymentStatus.PENDING
		);

		when(memberCommand.getMember(memberId))
			.thenReturn(member);

		when(cartItemCommand.getCartItemsByMemberIdAndIds(
			memberId,
			List.of(cartItemId)
		)).thenReturn(List.of(cartItem));

		when(productCommand.getProductsForOrder(List.of(productId)))
			.thenReturn(Map.of(
				productId,
				product
			));

		when(orderNumberGenerator.generate())
			.thenReturn("ORD-20260602-000001");

		when(orderRepository.save(any(Order.class)))
			.thenReturn(savedOrder);

		when(orderItemRepository.saveAll(anyList()))
			.thenReturn(List.of(savedOrderItem));

		when(paymentService.createPendingPayment(any(PaymentCreateCommand.class)))
			.thenReturn(payment);

		// when
		OrderCreateResponse response = orderService.createOrder(
			memberId,
			request
		);

		// then
		assertThat(response.orderNumber()).isEqualTo("ORD-20260602-000001");
		assertThat(response.memberId()).isEqualTo(memberId);
		assertThat(response.totalAmount()).isEqualTo(7000000L);
		assertThat(response.usedPointAmount()).isEqualTo(1000L);
		assertThat(response.finalPaymentAmount()).isEqualTo(6999000L);
		assertThat(response.paymentId()).isEqualTo("PAY-20260602-000001");
		assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.PENDING);
		assertThat(response.items()).hasSize(1);

		verify(member).usePoint(1000L);
		verify(product).removeStock(2L);
		verify(cartService).clearCart(memberId);
	}

	@Test
	@DisplayName("cartItemIds가 null이면 전체 장바구니 상품 기준으로 주문 생성에 성공한다")
	void createOrder_allCartItems_success() {
		// given
		Long memberId = 1L;
		Long productId = 1L;

		Member member = mock(Member.class);
		when(member.getId()).thenReturn(memberId);

		CartItem cartItem = mock(CartItem.class);
		when(cartItem.getProductId()).thenReturn(productId);
		when(cartItem.getQuantity()).thenReturn(1L);

		Product product = mock(Product.class);
		when(product.getId()).thenReturn(productId);
		when(product.getName()).thenReturn("아이폰 15 프로");
		when(product.getPrice()).thenReturn(1500000L);
		when(product.getStock()).thenReturn(50L);
		when(product.getStatus()).thenReturn(ProductStatus.ON_SALE);

		OrderCreateRequest request = new OrderCreateRequest(
			null,
			0L
		);

		Order savedOrder = Order.create(
			member,
			"ORD-20260602-000002",
			1500000L,
			0L
		);

		OrderItem savedOrderItem = mock(OrderItem.class);
		when(savedOrderItem.getId()).thenReturn(1L);
		when(savedOrderItem.getProductId()).thenReturn(productId);
		when(savedOrderItem.getProductName()).thenReturn("아이폰 15 프로");
		when(savedOrderItem.getOrderPrice()).thenReturn(1500000L);
		when(savedOrderItem.getQuantity()).thenReturn(1L);
		when(savedOrderItem.getTotalPrice()).thenReturn(1500000L);

		PaymentCreateResult payment = new PaymentCreateResult(
			"PAY-20260602-000002",
			memberId,
			1L,
			1500000L,
			0L,
			1500000L,
			PaymentStatus.PENDING
		);

		when(memberCommand.getMember(memberId))
			.thenReturn(member);

		when(cartItemCommand.getCartItemsByMemberId(memberId))
			.thenReturn(List.of(cartItem));

		when(productCommand.getProductsForOrder(List.of(productId)))
			.thenReturn(Map.of(
				productId,
				product
			));

		when(orderNumberGenerator.generate())
			.thenReturn("ORD-20260602-000002");

		when(orderRepository.save(any(Order.class)))
			.thenReturn(savedOrder);

		when(orderItemRepository.saveAll(anyList()))
			.thenReturn(List.of(savedOrderItem));

		when(paymentService.createPendingPayment(any(PaymentCreateCommand.class)))
			.thenReturn(payment);

		// when
		OrderCreateResponse response = orderService.createOrder(
			memberId,
			request
		);

		// then
		assertThat(response.totalAmount()).isEqualTo(1500000L);
		assertThat(response.usedPointAmount()).isEqualTo(0L);
		assertThat(response.finalPaymentAmount()).isEqualTo(1500000L);

		verify(member, never()).usePoint(0L);
		verify(product).removeStock(1L);
		verify(cartService).clearCart(memberId);
	}

	@Test
	@DisplayName("주문 생성 시 PG 실결제 금액은 주문 총액에서 사용 포인트를 뺀 금액이다")
	void createOrder_paymentFinalAmount_success() {
		// given
		Long memberId = 1L;
		Long cartItemId = 1L;
		Long productId = 1L;

		Member member = mock(Member.class);
		when(member.getId()).thenReturn(memberId);

		CartItem cartItem = mock(CartItem.class);
		when(cartItem.getProductId()).thenReturn(productId);
		when(cartItem.getQuantity()).thenReturn(2L);

		Product product = mock(Product.class);
		when(product.getId()).thenReturn(productId);
		when(product.getName()).thenReturn("맥북 프로 16인치");
		when(product.getPrice()).thenReturn(3500000L);
		when(product.getStock()).thenReturn(10L);
		when(product.getStatus()).thenReturn(ProductStatus.ON_SALE);

		OrderCreateRequest request = new OrderCreateRequest(
			List.of(cartItemId),
			1000L
		);

		Order savedOrder = Order.create(
			member,
			"ORD-20260602-000001",
			7000000L,
			1000L
		);

		OrderItem savedOrderItem = mock(OrderItem.class);
		when(savedOrderItem.getId()).thenReturn(1L);
		when(savedOrderItem.getProductId()).thenReturn(productId);
		when(savedOrderItem.getProductName()).thenReturn("맥북 프로 16인치");
		when(savedOrderItem.getOrderPrice()).thenReturn(3500000L);
		when(savedOrderItem.getQuantity()).thenReturn(2L);
		when(savedOrderItem.getTotalPrice()).thenReturn(7000000L);

		PaymentCreateResult payment = new PaymentCreateResult(
			"PAY-20260602-000001",
			memberId,
			1L,
			7000000L,
			1000L,
			6999000L,
			PaymentStatus.PENDING
		);

		when(memberCommand.getMember(memberId))
			.thenReturn(member);

		when(cartItemCommand.getCartItemsByMemberIdAndIds(
			memberId,
			List.of(cartItemId)
		)).thenReturn(List.of(cartItem));

		when(productCommand.getProductsForOrder(List.of(productId)))
			.thenReturn(Map.of(
				productId,
				product
			));

		when(orderNumberGenerator.generate())
			.thenReturn("ORD-20260602-000001");

		when(orderRepository.save(any(Order.class)))
			.thenReturn(savedOrder);

		when(orderItemRepository.saveAll(anyList()))
			.thenReturn(List.of(savedOrderItem));

		when(paymentService.createPendingPayment(any(PaymentCreateCommand.class)))
			.thenReturn(payment);

		ArgumentCaptor<PaymentCreateCommand> captor =
			ArgumentCaptor.forClass(PaymentCreateCommand.class);

		// when
		orderService.createOrder(
			memberId,
			request
		);

		// then
		verify(paymentService).createPendingPayment(captor.capture());

		PaymentCreateCommand command = captor.getValue();

		assertThat(command.memberId()).isEqualTo(memberId);
		assertThat(command.totalOrderAmount()).isEqualTo(7000000L);
		assertThat(command.usedPointAmount()).isEqualTo(1000L);
		assertThat(command.finalPaymentAmount()).isEqualTo(6999000L);
	}

	@Test
	@DisplayName("cartItemIds에 중복 ID가 포함되어도 중복 제거 후 주문 생성에 성공한다")
	void createOrder_duplicateCartItemIds_success() {
		// given
		Long memberId = 1L;
		Long cartItemId = 1L;
		Long productId = 1L;

		List<Long> requestedCartItemIds = List.of(cartItemId, cartItemId);
		List<Long> distinctCartItemIds = List.of(cartItemId);

		Member member = mock(Member.class);
		when(member.getId()).thenReturn(memberId);

		CartItem cartItem = mock(CartItem.class);
		when(cartItem.getProductId()).thenReturn(productId);
		when(cartItem.getQuantity()).thenReturn(1L);

		Product product = mock(Product.class);
		when(product.getId()).thenReturn(productId);
		when(product.getName()).thenReturn("맥북 프로 16인치");
		when(product.getPrice()).thenReturn(3500000L);
		when(product.getStock()).thenReturn(10L);
		when(product.getStatus()).thenReturn(ProductStatus.ON_SALE);

		OrderCreateRequest request = new OrderCreateRequest(
			requestedCartItemIds,
			0L
		);

		Order savedOrder = Order.create(
			member,
			"ORD-20260602-000003",
			3500000L,
			0L
		);

		OrderItem savedOrderItem = mock(OrderItem.class);
		when(savedOrderItem.getId()).thenReturn(1L);
		when(savedOrderItem.getProductId()).thenReturn(productId);
		when(savedOrderItem.getProductName()).thenReturn("맥북 프로 16인치");
		when(savedOrderItem.getOrderPrice()).thenReturn(3500000L);
		when(savedOrderItem.getQuantity()).thenReturn(1L);
		when(savedOrderItem.getTotalPrice()).thenReturn(3500000L);

		PaymentCreateResult payment = new PaymentCreateResult(
			"PAY-20260602-000003",
			memberId,
			1L,
			3500000L,
			0L,
			3500000L,
			PaymentStatus.PENDING
		);

		when(memberCommand.getMember(memberId))
			.thenReturn(member);

		when(cartItemCommand.getCartItemsByMemberIdAndIds(
			memberId,
			distinctCartItemIds
		)).thenReturn(List.of(cartItem));

		when(productCommand.getProductsForOrder(List.of(productId)))
			.thenReturn(Map.of(
				productId,
				product
			));

		when(orderNumberGenerator.generate())
			.thenReturn("ORD-20260602-000003");

		when(orderRepository.save(any(Order.class)))
			.thenReturn(savedOrder);

		when(orderItemRepository.saveAll(anyList()))
			.thenReturn(List.of(savedOrderItem));

		when(paymentService.createPendingPayment(any(PaymentCreateCommand.class)))
			.thenReturn(payment);

		// when
		OrderCreateResponse response = orderService.createOrder(
			memberId,
			request
		);

		// then
		assertThat(response.totalAmount()).isEqualTo(3500000L);

		verify(cartItemCommand).getCartItemsByMemberIdAndIds(
			memberId,
			distinctCartItemIds
		);
		verify(cartService).clearCart(memberId);
	}

	@Test
	@DisplayName("주문 생성 시 장바구니가 비어 있으면 예외가 발생한다")
	void createOrder_emptyCart_fail() {
		// given
		Long memberId = 1L;

		Member member = mock(Member.class);
		when(member.getId()).thenReturn(memberId);

		OrderCreateRequest request = new OrderCreateRequest(
			null,
			0L
		);

		when(memberCommand.getMember(memberId))
			.thenReturn(member);

		when(cartItemCommand.getCartItemsByMemberId(memberId))
			.thenReturn(List.of());

		// when & then
		assertThatThrownBy(() -> orderService.createOrder(
			memberId,
			request
		))
			.isInstanceOf(BusinessException.class)
			.hasMessage(OrderErrorCode.EMPTY_ORDER_ITEM.getMessage());

		verify(orderRepository, never()).save(any(Order.class));
		verify(paymentService, never()).createPendingPayment(any(PaymentCreateCommand.class));
		verify(cartService, never()).clearCart(memberId);
	}

	@Test
	@DisplayName("주문 생성 시 선택한 장바구니 상품 중 유효하지 않은 항목이 있으면 예외가 발생한다")
	void createOrder_invalidCartItem_fail() {
		// given
		Long memberId = 1L;
		List<Long> cartItemIds = List.of(1L, 2L, 999L);

		Member member = mock(Member.class);
		when(member.getId()).thenReturn(memberId);

		CartItem cartItem = mock(CartItem.class);

		OrderCreateRequest request = new OrderCreateRequest(
			cartItemIds,
			0L
		);

		when(memberCommand.getMember(memberId))
			.thenReturn(member);

		when(cartItemCommand.getCartItemsByMemberIdAndIds(
			memberId,
			cartItemIds
		)).thenReturn(List.of(cartItem));

		// when & then
		assertThatThrownBy(() -> orderService.createOrder(
			memberId,
			request
		))
			.isInstanceOf(BusinessException.class)
			.hasMessage(CartErrorCode.CART_ITEM_NOT_FOUND.getMessage());

		verify(orderRepository, never()).save(any(Order.class));
		verify(paymentService, never()).createPendingPayment(any(PaymentCreateCommand.class));
		verify(cartService, never()).clearCart(memberId);
	}

	@Test
	@DisplayName("주문 생성 시 상품이 판매 중 상태가 아니면 예외가 발생한다")
	void createOrder_notOnSaleProduct_fail() {
		// given
		Long memberId = 1L;
		Long productId = 1L;

		Member member = mock(Member.class);
		when(member.getId()).thenReturn(memberId);

		CartItem cartItem = mock(CartItem.class);
		when(cartItem.getProductId()).thenReturn(productId);

		Product product = mock(Product.class);
		when(product.getStatus()).thenReturn(ProductStatus.SOLD_OUT);

		OrderCreateRequest request = new OrderCreateRequest(
			null,
			0L
		);

		when(memberCommand.getMember(memberId))
			.thenReturn(member);

		when(cartItemCommand.getCartItemsByMemberId(memberId))
			.thenReturn(List.of(cartItem));

		when(productCommand.getProductsForOrder(List.of(productId)))
			.thenReturn(Map.of(
				productId,
				product
			));

		// when & then
		assertThatThrownBy(() -> orderService.createOrder(
			memberId,
			request
		))
			.isInstanceOf(BusinessException.class)
			.hasMessage(ProductErrorCode.PRODUCT_NOT_ON_SALE.getMessage());

		verify(orderRepository, never()).save(any(Order.class));
		verify(paymentService, never()).createPendingPayment(any(PaymentCreateCommand.class));
		verify(cartService, never()).clearCart(memberId);
	}

	@Test
	@DisplayName("주문 생성 시 상품 재고가 부족하면 예외가 발생한다")
	void createOrder_outOfStock_fail() {
		// given
		Long memberId = 1L;
		Long productId = 1L;

		Member member = mock(Member.class);
		when(member.getId()).thenReturn(memberId);

		CartItem cartItem = mock(CartItem.class);
		when(cartItem.getProductId()).thenReturn(productId);
		when(cartItem.getQuantity()).thenReturn(2L);

		Product product = mock(Product.class);
		when(product.getStatus()).thenReturn(ProductStatus.ON_SALE);
		when(product.getStock()).thenReturn(1L);

		OrderCreateRequest request = new OrderCreateRequest(
			null,
			0L
		);

		when(memberCommand.getMember(memberId))
			.thenReturn(member);

		when(cartItemCommand.getCartItemsByMemberId(memberId))
			.thenReturn(List.of(cartItem));

		when(productCommand.getProductsForOrder(List.of(productId)))
			.thenReturn(Map.of(
				productId,
				product
			));

		// when & then
		assertThatThrownBy(() -> orderService.createOrder(
			memberId,
			request
		))
			.isInstanceOf(BusinessException.class)
			.hasMessage(ProductErrorCode.OUT_OF_STOCK.getMessage());

		verify(product, never()).removeStock(2L);
		verify(orderRepository, never()).save(any(Order.class));
		verify(paymentService, never()).createPendingPayment(any(PaymentCreateCommand.class));
		verify(cartService, never()).clearCart(memberId);
	}

	@Test
	@DisplayName("주문 생성 시 사용 포인트가 주문 총액보다 크면 예외가 발생한다")
	void createOrder_usedPointGreaterThanTotalAmount_fail() {
		// given
		Long memberId = 1L;
		Long productId = 1L;

		Member member = mock(Member.class);
		when(member.getId()).thenReturn(memberId);

		CartItem cartItem = mock(CartItem.class);
		when(cartItem.getProductId()).thenReturn(productId);
		when(cartItem.getQuantity()).thenReturn(1L);

		Product product = mock(Product.class);
		when(product.getStatus()).thenReturn(ProductStatus.ON_SALE);
		when(product.getStock()).thenReturn(10L);
		when(product.getPrice()).thenReturn(10000L);

		OrderCreateRequest request = new OrderCreateRequest(
			null,
			20000L
		);

		when(memberCommand.getMember(memberId))
			.thenReturn(member);

		when(cartItemCommand.getCartItemsByMemberId(memberId))
			.thenReturn(List.of(cartItem));

		when(productCommand.getProductsForOrder(List.of(productId)))
			.thenReturn(Map.of(
				productId,
				product
			));

		// when & then
		assertThatThrownBy(() -> orderService.createOrder(
			memberId,
			request
		))
			.isInstanceOf(BusinessException.class)
			.hasMessage(PointErrorCode.INSUFFICIENT_POINT.getMessage());

		verify(member, never()).usePoint(20000L);
		verify(product, never()).removeStock(1L);
		verify(orderRepository, never()).save(any(Order.class));
		verify(paymentService, never()).createPendingPayment(any(PaymentCreateCommand.class));
		verify(cartService, never()).clearCart(memberId);
	}

	@Test
	@DisplayName("주문 생성 시 보유 포인트가 부족하면 예외가 발생한다")
	void createOrder_insufficientPoint_fail() {
		// given
		Long memberId = 1L;
		Long productId = 1L;

		Member member = mock(Member.class);
		when(member.getId()).thenReturn(memberId);

		CartItem cartItem = mock(CartItem.class);
		when(cartItem.getProductId()).thenReturn(productId);
		when(cartItem.getQuantity()).thenReturn(1L);

		Product product = mock(Product.class);
		when(product.getStatus()).thenReturn(ProductStatus.ON_SALE);
		when(product.getStock()).thenReturn(10L);
		when(product.getPrice()).thenReturn(10000L);

		OrderCreateRequest request = new OrderCreateRequest(
			null,
			5000L
		);

		when(memberCommand.getMember(memberId))
			.thenReturn(member);

		when(cartItemCommand.getCartItemsByMemberId(memberId))
			.thenReturn(List.of(cartItem));

		when(productCommand.getProductsForOrder(List.of(productId)))
			.thenReturn(Map.of(
				productId,
				product
			));

		doThrow(new BusinessException(PointErrorCode.INSUFFICIENT_POINT))
			.when(member)
			.usePoint(5000L);

		// when & then
		assertThatThrownBy(() -> orderService.createOrder(
			memberId,
			request
		))
			.isInstanceOf(BusinessException.class)
			.hasMessage(PointErrorCode.INSUFFICIENT_POINT.getMessage());

		verify(product, never()).removeStock(1L);
		verify(orderRepository, never()).save(any(Order.class));
		verify(paymentService, never()).createPendingPayment(any(PaymentCreateCommand.class));
		verify(cartService, never()).clearCart(memberId);
	}
}