package com.commercepaymentsystem.domain.product.dto;

import com.commercepaymentsystem.domain.product.entity.Product;
import com.commercepaymentsystem.domain.product.entity.ProductCategory;
import com.commercepaymentsystem.domain.product.entity.ProductStatus;

public record ProductDetailResponse(
	Long id,
	String name,
	Long price,
	Long stock,
	String description,
	ProductCategory category,
	ProductStatus status
) {
	public static ProductDetailResponse from(Product product) {
		return new ProductDetailResponse(
			product.getId(),
			product.getName(),
			product.getPrice(),
			product.getStock(),
			product.getDescription(),
			product.getCategory(),
			product.getStatus()
		);
	}
}
