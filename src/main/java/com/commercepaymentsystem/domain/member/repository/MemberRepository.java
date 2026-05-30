package com.commercepaymentsystem.domain.member.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.commercepaymentsystem.domain.member.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {

	boolean existsByEmailAndDeletedAtIsNull(String email);

	Optional<Member> findByEmailAndDeletedAtIsNull(String email);

	Optional<Member> findByIdAndDeletedAtIsNull(Long id);
}