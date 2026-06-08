package com.commercepaymentsystem.domain.payment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.commercepaymentsystem.domain.payment.entity.Payment;
import com.commercepaymentsystem.domain.payment.entity.PaymentStatus;

import jakarta.persistence.LockModeType;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

	Optional<Payment> findByPaymentId(String paymentId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Payment p where p.paymentId = :paymentId")
	Optional<Payment> findByPaymentIdForUpdate(@Param("paymentId") String paymentId);

	boolean existsByPaymentId(String paymentId);

	Optional<Payment> findByOrderId(Long orderId);

	List<Payment> findByMemberId(Long memberId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Payment p where p.orderId = :orderId")
	Optional<Payment> findByOrderIdForUpdate(@Param("orderId") Long orderId);

	@Query("""
		select coalesce(sum(p.finalPaymentAmount), 0)
		from Payment p
		where p.memberId = :memberId
		  and p.status in :statuses
		""")
	Long sumConfirmedFinalPaymentAmountByMemberId(
		@Param("memberId") Long memberId,
		@Param("statuses") List<PaymentStatus> statuses
	);

	default Long sumConfirmedFinalPaymentAmountByMemberId(Long memberId) {
		return sumConfirmedFinalPaymentAmountByMemberId(
			memberId,
			List.of(
				PaymentStatus.CONFIRMED,
				PaymentStatus.PARTIAL_REFUNDED,
				PaymentStatus.REFUNDED
			)
		);
	}
}
