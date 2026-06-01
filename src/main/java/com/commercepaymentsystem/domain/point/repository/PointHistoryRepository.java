package com.commercepaymentsystem.domain.point.repository;

import com.commercepaymentsystem.domain.point.entity.PointHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {

	Page<PointHistory> findByMemberId(Long memberId, Pageable pageable);
}
