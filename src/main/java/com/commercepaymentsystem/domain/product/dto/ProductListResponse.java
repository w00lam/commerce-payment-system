package com.commercepaymentsystem.domain.product.dto;

import java.time.LocalDateTime;

import com.commercepaymentsystem.domain.product.entity.Product;
import com.commercepaymentsystem.domain.product.entity.ProductCategory;
import com.commercepaymentsystem.domain.product.entity.ProductStatus;

public record ProductListResponse(
	Long id,
	String name,
	Long price,
	ProductStatus status,
	ProductCategory category,
	LocalDateTime createdAt
) {
	public static ProductListResponse from(Product product) {
		return new ProductListResponse(
			product.getId(),
			product.getName(),
			product.getPrice(),
			product.getStatus(),
			product.getCategory(),
			product.getCreatedAt()
		);
	}
}
