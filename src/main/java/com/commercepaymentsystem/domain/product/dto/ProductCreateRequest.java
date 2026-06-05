package com.commercepaymentsystem.domain.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.commercepaymentsystem.domain.product.entity.ProductCategory;
import com.commercepaymentsystem.domain.product.entity.ProductStatus;

public record ProductCreateRequest(
    @NotBlank @Size(max = 100) String name,
    @NotNull @Min(0) Long price,
    @NotNull @Min(0) Long stock,
    @Size(max = 255) String description,
    @NotNull ProductStatus status,
    @NotNull ProductCategory category
) {}
