package com.commercepaymentsystem.domain.point.repository;

import com.commercepaymentsystem.domain.point.entity.PointHistory;
import com.commercepaymentsystem.domain.point.entity.PointHistoryType;
import com.commercepaymentsystem.domain.point.entity.PointSourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {

	Page<PointHistory> findByMemberId(Long memberId, Pageable pageable);

	boolean existsByPaymentIdAndType(Long paymentId, PointHistoryType type);

	boolean existsByPaymentIdAndTypeAndSourceType(Long paymentId, PointHistoryType type, PointSourceType sourceType);

	boolean existsByPaymentIdAndTypeAndRefundId(Long paymentId, PointHistoryType type, Long refundId);

	boolean existsByPaymentIdAndTypeAndRefundIdAndSourceType(Long paymentId, PointHistoryType type, Long refundId, PointSourceType sourceType);

	@Query("select coalesce(sum(p.amount), 0) from PointHistory p where p.paymentId = :paymentId and p.type = :type")
	long sumAmountByPaymentIdAndType(@Param("paymentId") Long paymentId, @Param("type") PointHistoryType type);

	@Query("select coalesce(sum(p.amount), 0) from PointHistory p where p.paymentId = :paymentId and p.type = :type and p.sourceType = :sourceType")
	long sumAmountByPaymentIdAndTypeAndSourceType(@Param("paymentId") Long paymentId, @Param("type") PointHistoryType type, @Param("sourceType") PointSourceType sourceType);
}
