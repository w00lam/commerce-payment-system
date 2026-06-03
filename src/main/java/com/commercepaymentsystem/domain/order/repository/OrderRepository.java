package com.commercepaymentsystem.domain.order.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.commercepaymentsystem.domain.order.entity.Order;


public interface OrderRepository extends JpaRepository<Order, Long> {

	Page<Order> findByMember_Id(Long memberId, Pageable pageable);
}
