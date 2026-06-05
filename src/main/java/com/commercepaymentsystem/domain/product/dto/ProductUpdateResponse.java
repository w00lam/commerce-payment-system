package com.commercepaymentsystem.domain.product.dto;

import java.time.LocalDateTime;

import com.commercepaymentsystem.domain.product.entity.Product;

public record ProductUpdateResponse(
    Long id,
    String name,
    Long price,
    LocalDateTime updatedAt
) {
    public static ProductUpdateResponse from(Product product) {
        return new ProductUpdateResponse(
            product.getId(),
            product.getName(),
            product.getPrice(),
            product.getUpdatedAt()
        );
    }
}
