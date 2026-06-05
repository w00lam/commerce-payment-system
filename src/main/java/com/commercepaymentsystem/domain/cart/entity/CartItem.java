package com.commercepaymentsystem.domain.cart.entity;

import java.time.LocalDateTime;

import com.commercepaymentsystem.domain.cart.exception.CartErrorCode;
import com.commercepaymentsystem.global.entity.BaseEntity;
import com.commercepaymentsystem.global.exception.BusinessException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

/**
 * 장바구니에 담긴 개별 상품(Item)을 나타내는 엔티티입니다.
 * Cart와 Product 간의 다대다 관계를 일대다-다대일로 풀어서 매핑합니다.
 */
@Entity
@Getter
@Table(name = "cart_items", uniqueConstraints = {
    @UniqueConstraint(
        name = "uk_cart_product",
        columnNames = {"cart_id", "product_id"}
    )
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CartItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Long quantity;

    /**
     * 정적 팩토리 메서드: 장바구니 상품을 생성합니다.
     * 유효하지 않은 수량(0 이하) 입력 시 예외를 발생시켜 객체 생성 자체를 차단합니다.
     */
    public static CartItem create(Cart cart, Long productId, Long quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BusinessException(CartErrorCode.INVALID_QUANTITY);
        }
        return new CartItem(null, cart, productId, quantity);
    }

    /**
     * 동일 상품을 장바구니에 다시 담을 때 기존 수량에 합산합니다.
     */
    public void addQuantity(Long amount) {
        if (amount == null || amount <= 0) {
            throw new BusinessException(CartErrorCode.INVALID_QUANTITY);
        }
        this.quantity += amount;
    }

    /**
     * 장바구니 상품의 수량을 변경합니다. (예: 장바구니 화면에서 직접 수량 수정)
     */
    public void updateQuantity(Long quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BusinessException(CartErrorCode.INVALID_QUANTITY);
        }
        this.quantity = quantity;
    }
}
