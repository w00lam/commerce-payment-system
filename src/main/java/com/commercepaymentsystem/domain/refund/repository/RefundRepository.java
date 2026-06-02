package com.commercepaymentsystem.domain.refund.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import com.commercepaymentsystem.domain.refund.entity.Refund;

public interface RefundRepository extends JpaRepository<Refund, Long> {

	List<Refund> findByPaymentId(Long paymentId);
}
