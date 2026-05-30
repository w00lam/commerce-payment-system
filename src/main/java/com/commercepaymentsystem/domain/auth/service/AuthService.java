package com.commercepaymentsystem.domain.auth.service;

import lombok.RequiredArgsConstructor;

import org.hibernate.service.spi.ServiceException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commercepaymentsystem.domain.auth.dto.SignupRequest;
import com.commercepaymentsystem.domain.auth.dto.SignupResponse;
import com.commercepaymentsystem.domain.member.entity.Member;
import com.commercepaymentsystem.domain.member.exception.MemberErrorCode;
import com.commercepaymentsystem.domain.member.repository.MemberRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;

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
		if (memberRepository.existsByEmailAndDeletedAtIsNull(email)) {
			throw new ServiceException(MemberErrorCode.DUPLICATED_EMAIL.getMessage());
		}
	}
}