package com.commercepaymentsystem.domain.cart.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.commercepaymentsystem.domain.cart.entity.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);

    List<CartItem> findAllByCartId(Long cartId);

    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.memberId = :memberId AND ci.id = :cartItemId")
    Optional<CartItem> findByIdAndMemberId(@Param("cartItemId") Long cartItemId, @Param("memberId") Long memberId);

    @Query("""
    SELECT ci
    FROM CartItem ci
    JOIN FETCH ci.cart c
    WHERE c.memberId = :memberId
    """)
    List<CartItem> findAllByMemberId(@Param("memberId") Long memberId);

    @Query("""
    SELECT ci
    FROM CartItem ci
    JOIN FETCH ci.cart c
    WHERE c.memberId = :memberId
    AND ci.id IN :cartItemIds
    """)
    List<CartItem> findAllByMemberIdAndIdIn(
        @Param("memberId") Long memberId,
        @Param("cartItemIds") List<Long> cartItemIds
    );
}
