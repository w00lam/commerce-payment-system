package com.commercepaymentsystem.domain.order.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.commercepaymentsystem.domain.order.entity.Order;

import jakarta.persistence.LockModeType;

public interface OrderRepository extends JpaRepository<Order, Long> {

	Page<Order> findByMember_Id(Long memberId, Pageable pageable);

	// 주문 상세 조회에 사용
	@Query("""
		SELECT DISTINCT o
		FROM Order o
		LEFT JOIN FETCH o.orderItems
		WHERE o.id = :orderId
		  AND o.member.id = :memberId
	""")
	Optional<Order> findByIdAndMemberIdWithOrderItems(
		@Param("orderId") Long orderId,
		@Param("memberId") Long memberId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
	SELECT DISTINCT o
	FROM Order o
	LEFT JOIN FETCH o.orderItems
	WHERE o.id = :orderId
	  AND o.member.id = :memberId
""")
	Optional<Order> findByIdAndMemberIdWithOrderItemsForUpdate(
		@Param("orderId") Long orderId,
		@Param("memberId") Long memberId
	);
}
