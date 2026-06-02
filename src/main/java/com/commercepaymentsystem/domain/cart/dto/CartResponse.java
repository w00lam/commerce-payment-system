package com.commercepaymentsystem.domain.cart.dto;

import java.util.List;

public record CartResponse(
    Long cartId,
    Long memberId,
    List<CartItemDto> items,
    Long totalAmount
) {
    public static CartResponse of(Long cartId, Long memberId, List<CartItemDto> items, Long totalAmount) {
        return new CartResponse(cartId, memberId, items, totalAmount);
    }
}
