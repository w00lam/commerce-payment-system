package com.commercepaymentsystem.domain.membership.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.commercepaymentsystem.domain.membership.entity.MemberMembership;

import jakarta.persistence.LockModeType;

public interface MemberMembershipRepository	extends JpaRepository<MemberMembership, Long> {

	@Query("""
            select mm
            from MemberMembership mm
            where mm.member.id = :memberId
            """)
	Optional<MemberMembership> findByMemberId(Long memberId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
            select mm
            from MemberMembership mm
            where mm.member.id = :memberId
            """)
	Optional<MemberMembership> findByMemberIdForUpdate(
		@Param("memberId") Long memberId
	);
}
