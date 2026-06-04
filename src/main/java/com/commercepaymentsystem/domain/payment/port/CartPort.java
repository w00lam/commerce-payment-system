package com.commercepaymentsystem.domain.payment.port;

import java.util.List;

public interface CartPort {

	void deleteOrderedCartItems(Long memberId, List<Long> cartItemIds);
}
