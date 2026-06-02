package com.commercepaymentsystem.domain.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.commercepaymentsystem.domain.order.entity.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}