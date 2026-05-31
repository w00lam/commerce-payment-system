package com.commercepaymentsystem.domain.product.dto;

public record ProductDeleteResponse(
    Long id,
    String message
) {
    public static ProductDeleteResponse of(Long id) {
        return new ProductDeleteResponse(id, "정상적으로 삭제 처리되었습니다.");
    }
}
