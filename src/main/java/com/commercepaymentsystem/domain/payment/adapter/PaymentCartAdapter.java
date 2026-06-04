package com.commercepaymentsystem.domain.payment.adapter;

import java.util.List;

import org.springframework.stereotype.Component;

import com.commercepaymentsystem.domain.cart.service.CartService;
import com.commercepaymentsystem.domain.payment.port.CartPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentCartAdapter implements CartPort {

	private final CartService cartService;

	@Override
	public void deleteOrderedCartItems(Long memberId, List<Long> cartItemIds) {
		cartService.clearCartItems(cartItemIds, memberId);
	}
}
