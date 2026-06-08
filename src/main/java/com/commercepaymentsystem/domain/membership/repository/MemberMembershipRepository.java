package com.commercepaymentsystem.domain.membership.repository;

import com.commercepaymentsystem.domain.membership.entity.MemberMembership;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberMembershipRepository extends JpaRepository<MemberMembership, Long> {
	Optional<MemberMembership> findByMemberId(Long memberId);
}
