package com.commercepaymentsystem.domain.order.entity;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.commercepaymentsystem.domain.member.entity.Member;

class OrderTest {

	@Test
	@DisplayName("이미 확정된 주문을 다시 확정해도 예외를 던지지 않는다")
	void markAsConfirmed_alreadyConfirmed_idempotent() {
		Order order = order();
		order.markAsConfirmed();

		assertThatCode(order::markAsConfirmed).doesNotThrowAnyException();
		assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
	}

	private Order order() {
		Member member = Member.create(
			"member@example.com",
			"password",
			"member",
			"01012345678"
		);

		return new Order(member, 10_000L, List.of(), 0L, "order-number");
	}
}
