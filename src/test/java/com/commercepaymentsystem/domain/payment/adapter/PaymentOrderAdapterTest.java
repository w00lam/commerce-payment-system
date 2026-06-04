package com.commercepaymentsystem.domain.payment.adapter;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.commercepaymentsystem.domain.member.entity.Member;
import com.commercepaymentsystem.domain.order.entity.Order;
import com.commercepaymentsystem.domain.order.entity.OrderItem;
import com.commercepaymentsystem.domain.order.service.OrderService;
import com.commercepaymentsystem.domain.payment.port.OrderPort;
import com.commercepaymentsystem.domain.product.entity.Product;
import com.commercepaymentsystem.domain.product.entity.ProductCategory;
import com.commercepaymentsystem.domain.product.entity.ProductStatus;

class PaymentOrderAdapterTest {

	private final OrderService orderService = mock(OrderService.class);
	private final PaymentOrderAdapter paymentOrderAdapter = new PaymentOrderAdapter(orderService);

	@Test
	@DisplayName("주문 항목을 함께 조회해 주문 확정 후 원본 장바구니 항목 id를 반환한다")
	void confirmOrder_usesOrderItemsFetchQuery() {
		Order order = order();
		when(orderService.getOrderByIdWithOrderItems(10L)).thenReturn(order);

		OrderPort.ConfirmedOrder confirmedOrder = paymentOrderAdapter.confirmOrder(10L, 1L);

		verify(orderService).getOrderByIdWithOrderItems(10L);
		verify(orderService, never()).getOrderById(any());
		verify(orderService).validateOwner(order, 1L);
		verify(orderService).confirmOrder(order);
		assertThat(confirmedOrder.cartItemIds()).containsExactly(1000L);
	}

	private Order order() {
		Member member = Member.create(
			"member@example.com",
			"password",
			"member",
			"01012345678"
		);
		ReflectionTestUtils.setField(member, "id", 1L);

		Product product = Product.create(
			"Keyboard",
			10_000L,
			10L,
			"Keyboard",
			ProductStatus.ON_SALE,
			ProductCategory.ELECTRONICS
		);
		ReflectionTestUtils.setField(product, "id", 100L);

		OrderItem orderItem = new OrderItem(product, 10_000L, 1L, 1000L);
		Order order = new Order(member, 10_000L, List.of(orderItem), 0L, "order-number");
		ReflectionTestUtils.setField(order, "id", 10L);

		return order;
	}
}
