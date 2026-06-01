package com.commercepaymentsystem.domain.payment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.commercepaymentsystem.domain.payment.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

	Optional<Payment> findByPaymentId(String paymentId);

	boolean existsByPaymentId(String paymentId);

	Optional<Payment> findByOrderId(Long orderId);

	List<Payment> findByMemberId(Long memberId);
}
