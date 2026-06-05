package com.commercepaymentsystem.domain.cart.dto;

import com.commercepaymentsystem.domain.cart.entity.CartItem;
import com.commercepaymentsystem.domain.product.entity.Product;

public record CartItemDto(
    Long cartItemId,
    Long productId,
    String productName,
    Long price,
    Long quantity
) {
    public static CartItemDto of(CartItem cartItem, Product product) {
        return new CartItemDto(
            cartItem.getId(),
            cartItem.getProductId(),
            product.getName(),
            product.getPrice(),
            cartItem.getQuantity()
        );
    }
}
