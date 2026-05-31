package com.commercepaymentsystem.domain.product.dto;

import com.commercepaymentsystem.domain.product.entity.ProductCategory;
import com.commercepaymentsystem.domain.product.entity.ProductStatus;

public record ProductSearchCondition(
	ProductCategory category,
	ProductStatus status
) {
}
