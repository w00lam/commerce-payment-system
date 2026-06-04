package com.commercepaymentsystem.domain.payment.integration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.commercepaymentsystem.domain.cart.entity.Cart;
import com.commercepaymentsystem.domain.cart.entity.CartItem;
import com.commercepaymentsystem.domain.cart.repository.CartItemRepository;
import com.commercepaymentsystem.domain.cart.repository.CartRepository;
import com.commercepaymentsystem.domain.member.entity.Member;
import com.commercepaymentsystem.domain.member.repository.MemberRepository;
import com.commercepaymentsystem.domain.order.entity.Order;
import com.commercepaymentsystem.domain.order.entity.OrderStatus;
import com.commercepaymentsystem.domain.order.repository.OrderRepository;
import com.commercepaymentsystem.domain.order.service.OrderService;
import com.commercepaymentsystem.domain.payment.dto.PaymentConfirmCommand;
import com.commercepaymentsystem.domain.payment.dto.PaymentCreateCommand;
import com.commercepaymentsystem.domain.payment.dto.PaymentCreateResult;
import com.commercepaymentsystem.domain.payment.entity.Payment;
import com.commercepaymentsystem.domain.payment.entity.PaymentStatus;
import com.commercepaymentsystem.domain.payment.repository.PaymentRepository;
import com.commercepaymentsystem.domain.payment.service.PaymentService;
import com.commercepaymentsystem.domain.point.entity.PointHistoryType;
import com.commercepaymentsystem.domain.point.repository.PointHistoryRepository;
import com.commercepaymentsystem.domain.product.entity.Product;
import com.commercepaymentsystem.domain.product.entity.ProductCategory;
import com.commercepaymentsystem.domain.product.entity.ProductStatus;
import com.commercepaymentsystem.domain.product.repository.ProductRepository;
import com.commercepaymentsystem.infrastructure.portone.client.PortOneClient;
import com.commercepaymentsystem.infrastructure.portone.dto.PortOnePaymentResponse;

import jakarta.persistence.EntityManager;

@ActiveProfiles("test")
@Transactional
@SpringBootTest
class PaymentPostProcessIntegrationTest {

	@Autowired
	private PaymentService paymentService;

	@Autowired
	private OrderService orderService;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private CartRepository cartRepository;

	@Autowired
	private CartItemRepository cartItemRepository;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private PointHistoryRepository pointHistoryRepository;

	@Autowired
	private EntityManager entityManager;

	@MockitoBean
	private PortOneClient portOneClient;

	@Test
	@DisplayName("Payment confirmation updates order, points, and ordered cart items")
	void confirmPayment_postProcess_success() {
		Member member = memberRepository.save(member());
		member.addPoint(5_000L);

		Product orderedProduct = productRepository.save(product("Keyboard", 10_000L));
		Product remainingProduct = productRepository.save(product("Mouse", 3_000L));

		Cart cart = cartRepository.save(Cart.create(member.getId()));
		CartItem orderedCartItem = cartItemRepository.save(CartItem.create(cart, orderedProduct.getId(), 1L));
		CartItem remainingCartItem = cartItemRepository.save(CartItem.create(cart, remainingProduct.getId(), 1L));

		Order order = orderService.createOrder(
			member,
			List.of(orderedCartItem),
			List.of(orderedProduct),
			2_000L
		);
		PaymentCreateResult paymentCreateResult = paymentService.createPendingPayment(PaymentCreateCommand.from(order));
		Instant paidAt = Instant.parse("2026-06-01T01:02:03Z");

		when(portOneClient.getPayment(paymentCreateResult.paymentId()))
			.thenReturn(portOnePayment(
				paymentCreateResult.paymentId(),
				"order-" + order.getId(),
				paymentCreateResult.finalPaymentAmount(),
				paidAt
			));

		paymentService.confirmPayment(new PaymentConfirmCommand(paymentCreateResult.paymentId(), member.getId()));

		entityManager.flush();
		entityManager.clear();

		Payment payment = paymentRepository.findByPaymentId(paymentCreateResult.paymentId()).orElseThrow();
		Order confirmedOrder = orderRepository.findById(order.getId()).orElseThrow();
		Member confirmedMember = memberRepository.findById(member.getId()).orElseThrow();

		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
		assertThat(payment.getPaidAt()).isEqualTo(paidAt);
		assertThat(confirmedOrder.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
		assertThat(confirmedMember.getPointBalance()).isEqualTo(3_080L);
		assertThat(cartItemRepository.findById(orderedCartItem.getId())).isEmpty();
		assertThat(cartItemRepository.findById(remainingCartItem.getId())).isPresent();
		assertThat(pointHistoryRepository.findAll())
			.extracting("type")
			.containsExactlyInAnyOrder(PointHistoryType.USE, PointHistoryType.EARN);
	}

	private Member member() {
		return Member.create(
			"member@example.com",
			"password",
			"member",
			"01012345678"
		);
	}

	private Product product(String name, Long price) {
		return Product.create(
			name,
			price,
			10L,
			name,
			ProductStatus.ON_SALE,
			ProductCategory.ELECTRONICS
		);
	}

	private PortOnePaymentResponse portOnePayment(
		String paymentId,
		String orderName,
		Long totalAmount,
		Instant paidAt
	) {
		return new PortOnePaymentResponse(
			paymentId,
			"PAID",
			"transaction-123",
			orderName,
			new PortOnePaymentResponse.PortOnePaymentAmount(totalAmount),
			paidAt,
			null
		);
	}
}
