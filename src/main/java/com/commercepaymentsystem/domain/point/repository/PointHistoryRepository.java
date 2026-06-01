package com.commercepaymentsystem.domain.point.repository;

import com.commercepaymentsystem.domain.point.entity.PointHistory;
import com.commercepaymentsystem.domain.point.entity.PointHistoryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {

	Page<PointHistory> findByMemberId(Long memberId, Pageable pageable);

	boolean existsByPaymentIdAndType(Long paymentId, PointHistoryType type);
}
