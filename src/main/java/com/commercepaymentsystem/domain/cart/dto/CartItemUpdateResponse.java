package com.commercepaymentsystem.domain.cart.dto;

public record CartItemUpdateResponse(
    Long cartItemId,
    Long quantity
) {
    public static CartItemUpdateResponse of(Long cartItemId, Long quantity) {
        return new CartItemUpdateResponse(cartItemId, quantity);
    }
}
