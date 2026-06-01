package com.commercepaymentsystem.domain.cart.dto;

import com.commercepaymentsystem.domain.cart.entity.CartItem;

public record CartItemAddResponse(
    Long id,
    Long cartId,
    Long productId,
    Long quantity
) {
    public static CartItemAddResponse from(CartItem cartItem) {
        return new CartItemAddResponse(
            cartItem.getId(),
            cartItem.getCart().getId(),
            cartItem.getProductId(),
            cartItem.getQuantity()
        );
    }
}
