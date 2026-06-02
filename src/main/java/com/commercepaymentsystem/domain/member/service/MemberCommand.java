package com.commercepaymentsystem.domain.member.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.commercepaymentsystem.domain.member.entity.Member;
import com.commercepaymentsystem.domain.member.exception.MemberErrorCode;
import com.commercepaymentsystem.domain.member.repository.MemberRepository;
import com.commercepaymentsystem.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * 다른 도메인에서 Member 도메인에 접근하기 위한 파사드 컴포넌트입니다.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberCommand {

	private final MemberRepository memberRepository;

	/**
	 * 회원 ID로 회원을 조회합니다.
	 *
	 * @param memberId 회원 ID
	 * @return 조회된 회원
	 */
	public Member getMember(Long memberId) {
		return memberRepository.findByIdAndDeletedAtIsNull(memberId)
			.orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
	}
}