package com.commercepaymentsystem.domain.refund.adapter;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.commercepaymentsystem.domain.product.service.ProductService;
import com.commercepaymentsystem.domain.refund.port.RefundProductPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RefundProductAdapter implements RefundProductPort {

	private final ProductService productService;

	@Override
	public void restoreProductStocks(Map<Long, Long> productQuantities) {
		productService.restoreProductStocks(productQuantities);
	}
}
