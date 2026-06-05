package com.commercepaymentsystem.domain.member.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commercepaymentsystem.domain.member.dto.MemberDeleteRequest;
import com.commercepaymentsystem.domain.member.entity.Member;
import com.commercepaymentsystem.domain.member.exception.MemberErrorCode;
import com.commercepaymentsystem.domain.member.repository.MemberRepository;
import com.commercepaymentsystem.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;

	public Member getMember(Long memberId) {
		return memberRepository.findByIdAndDeletedAtIsNull(memberId)
			.orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
	}

	/**
	 * 인증된 회원의 탈퇴 요청을 처리합니다.
	 *
	 * JWT에서 추출한 memberId로 회원을 조회한 뒤, 요청 비밀번호와 저장된 암호화 비밀번호를 검증합니다.
	 * 검증에 성공하면 deletedAt 값을 기록하여 soft delete 처리합니다.
	 *
	 * @param memberId 인증된 회원 ID
	 * @param request 회원 탈퇴 요청 정보
	 * @throws BusinessException 회원을 찾을 수 없거나 비밀번호가 일치하지 않는 경우
	 */
	@Transactional
	public void deleteMyAccount(
		Long memberId,
		MemberDeleteRequest request
	) {
		Member member = getMember(memberId);

		validatePassword(
			request.password(),
			member.getPassword()
		);

		member.delete();
	}

	/**
	 * 입력된 비밀번호와 저장된 암호화 비밀번호가 일치하는지 검증합니다.
	 *
	 * @param rawPassword 사용자가 입력한 평문 비밀번호
	 * @param encodedPassword DB에 저장된 암호화 비밀번호
	 * @throws BusinessException 비밀번호가 일치하지 않는 경우
	 */
	private void validatePassword(
		String rawPassword,
		String encodedPassword
	) {
		if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
			throw new BusinessException(MemberErrorCode.INVALID_PASSWORD);
		}
	}
}
