package com.commercepaymentsystem.domain.product.dto;

import java.time.LocalDateTime;

import com.commercepaymentsystem.domain.product.entity.Product;
import com.commercepaymentsystem.domain.product.entity.ProductCategory;
import com.commercepaymentsystem.domain.product.entity.ProductStatus;

public record ProductCreateResponse(
    Long id,
    String name,
    Long price,
    Long stock,
    String description,
    ProductStatus status,
    ProductCategory category,
    LocalDateTime createdAt
) {
    public static ProductCreateResponse from(Product product) {
        return new ProductCreateResponse(
            product.getId(),
            product.getName(),
            product.getPrice(),
            product.getStock(),
            product.getDescription(),
            product.getStatus(),
            product.getCategory(),
            product.getCreatedAt()
        );
    }
}
