package com.commercepaymentsystem.domain.cart.dto;

public record CartItemDeleteResponse(
    Long cartItemId,
    String message
) {
    public static CartItemDeleteResponse of(Long cartItemId) {
        return new CartItemDeleteResponse(
            cartItemId,
            "장바구니에서 상품이 삭제되었습니다."
        );
    }
}
