package com.commercepaymentsystem.domain.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.commercepaymentsystem.domain.member.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {

	boolean existsByEmail(String email);
}