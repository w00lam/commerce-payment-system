package com.commercepaymentsystem.domain.refund.port;

import java.util.Map;

public interface RefundProductPort {

	void restoreProductStocks(Map<Long, Long> productQuantities);
}
