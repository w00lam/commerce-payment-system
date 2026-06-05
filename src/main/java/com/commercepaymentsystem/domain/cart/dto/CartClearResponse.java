package com.commercepaymentsystem.domain.cart.dto;

public record CartClearResponse(
    Long cartId,
    String message
) {
    public static CartClearResponse of(Long cartId) {
        return new CartClearResponse(
            cartId,
            "장바구니가 성공적으로 비워졌습니다."
        );
    }
}
