package com.commercepaymentsystem.domain.refund.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.commercepaymentsystem.domain.refund.entity.Refund;
import com.commercepaymentsystem.domain.refund.entity.RefundStatus;

public interface RefundRepository extends JpaRepository<Refund, Long> {

	List<Refund> findByPaymentId(Long paymentId);

	Optional<Refund> findFirstByPaymentIdAndStatusOrderByIdAsc(Long paymentId, RefundStatus status);

	@Query("""
		select coalesce(sum(r.pgRefundAmount), 0)
		from Refund r
		join Payment p on p.id = r.paymentId
		where p.memberId = :memberId
		  and r.status = :status
		""")
	Long sumCompletedRefundAmountByMemberId(
		@Param("memberId") Long memberId,
		@Param("status") RefundStatus status
	);

	default Long sumCompletedRefundAmountByMemberId(Long memberId) {
		return sumCompletedRefundAmountByMemberId(
			memberId,
			RefundStatus.COMPLETED
		);
	}
}
