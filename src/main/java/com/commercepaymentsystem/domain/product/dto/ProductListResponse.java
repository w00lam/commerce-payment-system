package com.commercepaymentsystem.domain.product.dto;

import com.commercepaymentsystem.domain.product.entity.Product;
import com.commercepaymentsystem.domain.product.entity.ProductCategory;
import com.commercepaymentsystem.domain.product.entity.ProductStatus;

public record ProductListResponse(
	Long id,
	String name,
	Long price,
	Long stock,
	ProductCategory category,
	ProductStatus status
) {
	public static ProductListResponse from(Product product) {
		return new ProductListResponse(
			product.getId(),
			product.getName(),
			product.getPrice(),
			product.getStock(),
			product.getCategory(),
			product.getStatus()
		);
	}
}
