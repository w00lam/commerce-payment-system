package com.commercepaymentsystem.domain.payment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.commercepaymentsystem.domain.payment.entity.Payment;

import jakarta.persistence.LockModeType;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

	Optional<Payment> findByPaymentId(String paymentId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Payment p where p.paymentId = :paymentId")
	Optional<Payment> findByPaymentIdForUpdate(@Param("paymentId") String paymentId);

	boolean existsByPaymentId(String paymentId);

	Optional<Payment> findByOrderId(Long orderId);

	List<Payment> findByMemberId(Long memberId);
}
