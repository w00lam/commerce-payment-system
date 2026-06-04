package com.commercepaymentsystem.domain.refund.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import com.commercepaymentsystem.domain.refund.entity.Refund;
import com.commercepaymentsystem.domain.refund.entity.RefundStatus;

public interface RefundRepository extends JpaRepository<Refund, Long> {

	List<Refund> findByPaymentId(Long paymentId);

	Optional<Refund> findFirstByPaymentIdAndStatusOrderByIdAsc(Long paymentId, RefundStatus status);
}
