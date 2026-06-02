package com.commercepaymentsystem.domain.cart.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.commercepaymentsystem.domain.cart.dto.CartClearResponse;
import com.commercepaymentsystem.domain.cart.dto.CartItemAddRequest;
import com.commercepaymentsystem.domain.cart.dto.CartItemAddResponse;
import com.commercepaymentsystem.domain.cart.dto.CartItemDeleteResponse;
import com.commercepaymentsystem.domain.cart.dto.CartItemQuantityUpdateRequest;
import com.commercepaymentsystem.domain.cart.dto.CartItemUpdateResponse;
import com.commercepaymentsystem.domain.cart.dto.CartResponse;
import com.commercepaymentsystem.domain.cart.service.CartService;
import com.commercepaymentsystem.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;

/**
 * 장바구니 관련 API 요청을 처리하는 컨트롤러입니다.
 * 인증된 회원(@AuthenticationPrincipal)만 접근 가능하며,
 * 모든 응답은 ApiResponse 규격에 맞춰 반환됩니다.
 */
@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /**
     * 1. 장바구니 상품 담기
     */
    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CartItemAddResponse> addCartItem(
        @AuthenticationPrincipal Long memberId,
        @Valid @RequestBody CartItemAddRequest request
    ) {
        return ApiResponse.ok(cartService.addCartItem(memberId, request));
    }

    /**
     * 2. 내 장바구니 전체 조회
     */
    @GetMapping
    public ApiResponse<CartResponse> getMyCart(
        @AuthenticationPrincipal Long memberId
    ) {
        return ApiResponse.ok(cartService.getMyCart(memberId));
    }

    /**
     * 3. 장바구니 상품 수량 변경
     */
    @PutMapping("/items/{cartItemId}")
    public ApiResponse<CartItemUpdateResponse> updateCartItemQuantity(
        @AuthenticationPrincipal Long memberId,
        @PathVariable Long cartItemId,
        @Valid @RequestBody CartItemQuantityUpdateRequest request
    ) {
        return ApiResponse.ok(cartService.updateCartItemQuantity(memberId, cartItemId, request));
    }

    /**
     * 4. 장바구니 특정 상품 삭제
     */
    @DeleteMapping("/items/{cartItemId}")
    public ApiResponse<CartItemDeleteResponse> deleteCartItem(
        @AuthenticationPrincipal Long memberId,
        @PathVariable Long cartItemId
    ) {
        return ApiResponse.ok(cartService.deleteCartItem(memberId, cartItemId));
    }

    /**
     * 5. 장바구니 전체 비우기
     */
    @DeleteMapping("/items")
    public ApiResponse<CartClearResponse> clearCart(
        @AuthenticationPrincipal Long memberId
    ) {
        return ApiResponse.ok(cartService.clearCart(memberId));
    }
}
