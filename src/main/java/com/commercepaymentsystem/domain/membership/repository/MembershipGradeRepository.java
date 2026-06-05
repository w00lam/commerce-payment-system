package com.commercepaymentsystem.domain.membership.repository;

import com.commercepaymentsystem.domain.membership.entity.MembershipGrade;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipGradeRepository extends JpaRepository<MembershipGrade, Long> {
	Optional<MembershipGrade> findByName(String name);
}
