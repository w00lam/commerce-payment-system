package com.commercepaymentsystem.domain.auth.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.commercepaymentsystem.domain.auth.dto.LoginRequest;
import com.commercepaymentsystem.domain.auth.dto.LoginResponse;
import com.commercepaymentsystem.domain.auth.dto.SignupRequest;
import com.commercepaymentsystem.domain.auth.dto.SignupResponse;
import com.commercepaymentsystem.domain.member.entity.Member;
import com.commercepaymentsystem.domain.member.exception.MemberErrorCode;
import com.commercepaymentsystem.domain.member.repository.MemberRepository;
import com.commercepaymentsystem.global.exception.BusinessException;
import com.commercepaymentsystem.global.jwt.JwtProvider;

class AuthServiceTest {

	private final MemberRepository memberRepository = org.mockito.Mockito.mock(MemberRepository.class);
	private final PasswordEncoder passwordEncoder = org.mockito.Mockito.mock(PasswordEncoder.class);
	private final JwtProvider jwtProvider = org.mockito.Mockito.mock(JwtProvider.class);

	private final AuthService authService = new AuthService(
		memberRepository,
		passwordEncoder,
		jwtProvider
	);

	@Test
	@DisplayName("회원가입 성공 시 비밀번호를 암호화하여 회원을 저장한다")
	void signup_success() {
		// given
		SignupRequest request = new SignupRequest(
			"user@example.com",
			"Password1234!",
			"홍길동",
			"010-1234-5678"
		);

		when(memberRepository.existsByEmail(request.email())).thenReturn(false);
		when(passwordEncoder.encode(request.password())).thenReturn("encodedPassword");
		when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// when
		SignupResponse response = authService.signup(request);

		// then
		ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
		verify(memberRepository).save(memberCaptor.capture());

		Member savedMember = memberCaptor.getValue();

		assertThat(savedMember.getEmail()).isEqualTo("user@example.com");
		assertThat(savedMember.getPassword()).isEqualTo("encodedPassword");
		assertThat(savedMember.getPointBalance()).isZero();

		assertThat(response.email()).isEqualTo("user@example.com");
		assertThat(response.name()).isEqualTo("홍길동");

	}

	@Test
	@DisplayName("이미 가입된 이메일이면 회원가입에 실패한다")
	void signup_duplicatedEmail_fail() {
		//given
		SignupRequest request = new SignupRequest(
			"user@example.com",
			"Password1234!",
			"홍길동",
			"010-1234-5678"
		);

		when(memberRepository.existsByEmail(request.email())).thenReturn(true);

		// when & then
		assertThatThrownBy(() -> authService.signup(request))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(MemberErrorCode.DUPLICATED_EMAIL);
	}

	@Test
	@DisplayName("로그인 성공 시 Access Token을 반환한다")
	void login_success() {
		// given
		LoginRequest request = new LoginRequest(
			"user@example.com",
			"Password1234!"
		);

		Member member = Member.create(
			"user@example.com",
			"encoded-password",
			"홍길동",
			"010-1234-5678"
		);

		when(memberRepository.findByEmailAndDeletedAtIsNull(request.email()))
			.thenReturn(Optional.of(member));
		when(passwordEncoder.matches(request.password(), member.getPassword()))
			.thenReturn(true);
		when(jwtProvider.createToken(member.getId(), member.getEmail()))
			.thenReturn("access-token");

		// when
		LoginResponse response = authService.login(request);

		// then
		assertThat(response.accessToken()).isEqualTo("access-token");
		assertThat(response.tokenType()).isEqualTo("Bearer");
		assertThat(response.member().email()).isEqualTo("user@example.com");
	}

	@Test
	@DisplayName("존재하지 않는 이메일이면 로그인에 실패한다")
	void login_notFoundEmail_fail() {
		// given
		LoginRequest request = new LoginRequest(
			"user@example.com",
			"Password1234!"
		);

		when(memberRepository.findByEmailAndDeletedAtIsNull(request.email()))
			.thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> authService.login(request))
			.isInstanceOf(BusinessException.class)
			.satisfies(exception -> {
				BusinessException businessException = (BusinessException)exception;
				assertThat(businessException.getErrorCode())
					.isEqualTo(MemberErrorCode.INVALID_LOGIN_INFO);
			});
	}

	@Test
	@DisplayName("비밀번호가 일치하지 않으면 로그인에 실패한다")
	void login_invalidPassword_fail() {
		// given
		LoginRequest request = new LoginRequest(
			"user@example.com",
			"WrongPassword123!"
		);

		Member member = Member.create(
			"user@example.com",
			"encoded-password",
			"홍길동",
			"010-1234-5678"
		);

		when(memberRepository.findByEmailAndDeletedAtIsNull(request.email()))
			.thenReturn(Optional.of(member));
		when(passwordEncoder.matches(request.password(), member.getPassword()))
			.thenReturn(false);

		// when & then
		assertThatThrownBy(() -> authService.login(request))
			.isInstanceOf(BusinessException.class)
			.satisfies(exception -> {
				BusinessException businessException = (BusinessException)exception;
				assertThat(businessException.getErrorCode())
					.isEqualTo(MemberErrorCode.INVALID_LOGIN_INFO);
			});
	}

}