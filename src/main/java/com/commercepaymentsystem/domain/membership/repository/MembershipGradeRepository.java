package com.commercepaymentsystem.domain.membership.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.commercepaymentsystem.domain.membership.entity.MembershipGrade;

public interface MembershipGradeRepository extends JpaRepository<MembershipGrade, Long> {

	Optional<MembershipGrade> findByName(String name);

	List<MembershipGrade> findAllByOrderByMinCumulativePaymentAmountAsc();

	Optional<MembershipGrade>
	findFirstByMinCumulativePaymentAmountLessThanEqualOrderByMinCumulativePaymentAmountDesc(
		Long cumulativePaymentAmount
	);
}
