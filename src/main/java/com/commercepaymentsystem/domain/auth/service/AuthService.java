package com.commercepaymentsystem.domain.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commercepaymentsystem.domain.auth.dto.SignupRequest;
import com.commercepaymentsystem.domain.auth.dto.SignupResponse;
import com.commercepaymentsystem.domain.member.entity.Member;
import com.commercepaymentsystem.domain.member.exception.MemberErrorCode;
import com.commercepaymentsystem.domain.member.repository.MemberRepository;
import com.commercepaymentsystem.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;

	/**
	 * 회원가입을 처리합니다.
	 *
	 * @param request 회원가입 요청 정보
	 * @return 가입된 회원 정보
	 * @throws BusinessException 이메일이 이미 존재하는 경우
	 */
	@Transactional
	public SignupResponse signup(SignupRequest request) {
		validateDuplicatedEmail(request.email());

		String encodedPassword = passwordEncoder.encode(request.password());

		Member member = Member.create(
			request.email(),
			encodedPassword,
			request.name(),
			request.phone()
		);

		Member savedMember = memberRepository.save(member);

		return SignupResponse.from(savedMember);
	}

	private void validateDuplicatedEmail(String email) {
		if (memberRepository.existsByEmail(email)) {
			throw new BusinessException(MemberErrorCode.DUPLICATED_EMAIL);
		}
	}
}