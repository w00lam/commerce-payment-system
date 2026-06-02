package com.commercepaymentsystem.domain.cart.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commercepaymentsystem.domain.cart.dto.CartClearResponse;
import com.commercepaymentsystem.domain.cart.dto.CartItemAddRequest;
import com.commercepaymentsystem.domain.cart.dto.CartItemAddResponse;
import com.commercepaymentsystem.domain.cart.dto.CartItemDeleteResponse;
import com.commercepaymentsystem.domain.cart.dto.CartItemDto;
import com.commercepaymentsystem.domain.cart.dto.CartItemQuantityUpdateRequest;
import com.commercepaymentsystem.domain.cart.dto.CartItemUpdateResponse;
import com.commercepaymentsystem.domain.cart.dto.CartResponse;
import com.commercepaymentsystem.domain.cart.entity.Cart;
import com.commercepaymentsystem.domain.cart.entity.CartItem;
import com.commercepaymentsystem.domain.cart.exception.CartErrorCode;
import com.commercepaymentsystem.domain.cart.repository.CartItemRepository;
import com.commercepaymentsystem.domain.cart.repository.CartRepository;
import com.commercepaymentsystem.domain.product.entity.Product;
import com.commercepaymentsystem.domain.product.service.ProductCommand;
import com.commercepaymentsystem.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * 장바구니 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 상품 담기, 조회, 수량 변경, 삭제 로직을 포함하며, 트랜잭션 단위로 관리됩니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductCommand productCommand;

    /**
     * 장바구니에 상품을 담습니다.
     * 이미 동일한 상품이 장바구니에 있다면 수량을 합산합니다.
     * 담으려는 총 수량이 상품의 남은 재고를 초과하면 예외를 발생시킵니다.
     *
     * @param memberId 회원 ID
     * @param request  담을 상품 ID 및 수량
     * @return 추가된 장바구니 상품 정보 (CartItemAddResponse)
     */
    @Transactional
    public CartItemAddResponse addCartItem(Long memberId, CartItemAddRequest request) {
        // 1. 상품 조회 (없으면 예외 발생)
        Product product = productCommand.getProductForCart(request.productId());

        // 2. 내 장바구니 조회 (없으면 새로 생성)
        Cart cart = cartRepository.findByMemberId(memberId)
            .orElseGet(() -> cartRepository.save(Cart.create(memberId)));

        CartItem existingCartItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())
            .orElse(null);

        CartItem savedCartItem;
        if (existingCartItem != null) {
            long newQuantity = existingCartItem.getQuantity() + request.quantity();
            if (product.getStock() < newQuantity) {
                throw new BusinessException(CartErrorCode.OUT_OF_STOCK);
            }
            existingCartItem.addQuantity(request.quantity());
            savedCartItem = existingCartItem;
        } else {
            if (product.getStock() < request.quantity()) {
                throw new BusinessException(CartErrorCode.OUT_OF_STOCK);
            }
            savedCartItem = cartItemRepository.save(CartItem.create(cart, product.getId(), request.quantity()));
        }

        return CartItemAddResponse.from(savedCartItem);
    }

    /**
     * 내 장바구니에 담긴 모든 상품 목록을 조회합니다.
     * 장바구니가 없으면 빈 응답을 반환하여 CQRS 원칙(조회 시 부작용 없음)을 지킵니다.
     * N+1 쿼리 문제를 방지하기 위해 상품(Product) 정보는 한 번에 Bulk 쿼리(findAllById)로 조회합니다.
     *
     * @param memberId 회원 ID
     * @return 장바구니 정보 및 상품 목록 (CartResponse)
     */
    public CartResponse getMyCart(Long memberId) {
        return cartRepository.findByMemberId(memberId)
            .map(cart -> {
                List<CartItem> cartItems = cartItemRepository.findAllByCartId(cart.getId());
                
                List<Long> productIds = cartItems.stream()
                    .map(CartItem::getProductId)
                    .toList();
                    
                java.util.Map<Long, Product> productMap = productCommand.getProductsForCart(productIds);
                    
                List<CartItemDto> itemDtos = cartItems.stream()
                    .map(cartItem -> {
                        Product product = productMap.get(cartItem.getProductId());
                        if (product == null) {
                            // 상품이 삭제되었거나 존재하지 않는 경우 건너뜁니다.
                            return null;
                        }
                        return CartItemDto.of(cartItem, product);
                    })
                    .filter(java.util.Objects::nonNull)
                    .toList();
                    
                long totalAmount = itemDtos.stream()
                    .mapToLong(dto -> dto.price() * dto.quantity())
                    .sum();
                    
                return CartResponse.of(cart.getId(), memberId, itemDtos, totalAmount);
            })
            .orElseGet(() -> CartResponse.of(null, memberId, List.of(), 0L));
    }

    /**
     * 장바구니에 담긴 특정 상품의 수량을 변경합니다.
     * 본인의 장바구니 상품인지 검증하며, 변경할 수량이 상품의 남은 재고를 초과하면 예외를 발생시킵니다.
     *
     * @param memberId   회원 ID
     * @param cartItemId 변경할 장바구니 상품 ID
     * @param request    변경할 수량
     * @return 변경된 장바구니 상품 정보 (CartItemUpdateResponse)
     */
    @Transactional
    public CartItemUpdateResponse updateCartItemQuantity(Long memberId, Long cartItemId, CartItemQuantityUpdateRequest request) {
        // 1. 내 장바구니 상품인지 확인 및 조회
        CartItem cartItem = cartItemRepository.findByIdAndMemberId(cartItemId, memberId)
            .orElseThrow(() -> new BusinessException(CartErrorCode.CART_ITEM_NOT_FOUND));

        Product product = productCommand.getProductForCart(cartItem.getProductId());

        if (product.getStock() < request.quantity()) {
            throw new BusinessException(CartErrorCode.OUT_OF_STOCK);
        }

        cartItem.updateQuantity(request.quantity());
        return CartItemUpdateResponse.of(cartItem.getId(), cartItem.getQuantity());
    }

    /**
     * 장바구니에서 특정 상품을 단건 삭제(Soft Delete)합니다.
     * 본인의 장바구니 상품인지 검증 후 삭제 처리합니다.
     *
     * @param memberId   회원 ID
     * @param cartItemId 삭제할 장바구니 상품 ID
     * @return 삭제 완료 메시지 (CartItemDeleteResponse)
     */
    @Transactional
    public CartItemDeleteResponse deleteCartItem(Long memberId, Long cartItemId) {
        CartItem cartItem = cartItemRepository.findByIdAndMemberId(cartItemId, memberId)
            .orElseThrow(() -> new BusinessException(CartErrorCode.CART_ITEM_NOT_FOUND));

        cartItemRepository.delete(cartItem);
        return CartItemDeleteResponse.of(cartItem.getId());
    }

    /**
     * 내 장바구니에 담긴 모든 상품을 전체 삭제(비우기) 처리합니다.
     * HTTP DELETE 멱등성을 보장하기 위해 장바구니가 없더라도 새로 생성 후 200 OK를 반환합니다.
     *
     * @param memberId 회원 ID
     * @return 비우기 완료 메시지 (CartClearResponse)
     */
    @Transactional
    public CartClearResponse clearCart(Long memberId) {
        return cartRepository.findByMemberId(memberId)
            .map(cart -> {
                cartItemRepository.deleteAllInBatch(cartItemRepository.findAllByCartId(cart.getId()));
                return CartClearResponse.of(cart.getId());
            })
            .orElseGet(() -> CartClearResponse.of(null));
    }
}
