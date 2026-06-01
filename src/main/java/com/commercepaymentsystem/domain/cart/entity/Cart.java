package com.commercepaymentsystem.domain.cart.entity;

import java.time.LocalDateTime;

import com.commercepaymentsystem.global.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

/**
 * 회원의 장바구니를 나타내는 엔티티입니다.
 * 회원(Member)과 1:1 관계를 가집니다.
 */
@SQLRestriction("deleted_at IS NULL")
@Entity
@Getter
@Table(name = "carts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Cart extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false, unique = true)
    private Long memberId;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * 특정 회원의 장바구니를 생성합니다.
     */
    public static Cart create(Long memberId) {
        return new Cart(null, memberId, null);
    }
}
