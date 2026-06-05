package com.commercepaymentsystem.domain.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commercepaymentsystem.domain.auth.dto.LoginRequest;
import com.commercepaymentsystem.domain.auth.dto.LoginResponse;
import com.commercepaymentsystem.domain.auth.dto.SignupRequest;
import com.commercepaymentsystem.domain.auth.dto.SignupResponse;
import com.commercepaymentsystem.domain.member.entity.Member;
import com.commercepaymentsystem.domain.member.exception.MemberErrorCode;
import com.commercepaymentsystem.domain.member.repository.MemberRepository;
import com.commercepaymentsystem.global.exception.BusinessException;
import com.commercepaymentsystem.global.jwt.JwtProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtProvider jwtProvider;

	/**
	 * 회원가입 요청 정보를 검증하고 신규 회원을 생성합니다.
	 * 이메일 중복 여부를 확인한 뒤 비밀번호를 암호화하여 회원 정보를 저장합니다.
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

	/**
	 * 로그인 요청 정보를 검증하고 JWT Access Token을 발급합니다.
	 *
	 * 이메일로 회원을 조회한 뒤, 입력된 비밀번호와 저장된 암호화 비밀번호를 비교합니다.
	 * 검증에 성공하면 JWT Access Token을 생성하여 로그인 응답으로 반환합니다.
	 *
	 * @param request 로그인 요청 정보
	 * @return JWT Access Token과 로그인 회원 정보를 포함한 응답
	 * @throws BusinessException 이메일이 존재하지 않거나, 탈퇴/삭제된 회원이거나, 비밀번호가 일치하지 않는 경우
	 */
	public LoginResponse login(LoginRequest request) {
		Member member = memberRepository.findByEmailAndDeletedAtIsNull(request.email())
			.orElseThrow(() -> new BusinessException(MemberErrorCode.INVALID_LOGIN_INFO));

		validatePassword(
			request.password(),
			member.getPassword()
		);

		String accessToken = jwtProvider.createToken(
			member.getId(),
			member.getEmail()
		);

		return LoginResponse.of(
			accessToken,
			member
		);
	}

	/**
	 * 이메일 중복 여부를 검증합니다.
	 *
	 * 이미 동일한 이메일로 가입된 회원이 존재하는 경우 회원가입을 진행하지 않고
	 * {@link MemberErrorCode#DUPLICATED_EMAIL} 예외를 발생시킵니다.
	 *
	 * @param email 중복 검증할 이메일
	 * @throws BusinessException 이미 가입된 이메일인 경우
	 */
	private void validateDuplicatedEmail(String email) {
		if (memberRepository.existsByEmail(email)) {
			throw new BusinessException(MemberErrorCode.DUPLICATED_EMAIL);
		}
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
			throw new BusinessException(MemberErrorCode.INVALID_LOGIN_INFO);
		}
	}

	/**
	 * 로그아웃 요청을 처리합니다.
	 *
	 * 현재 인증 구조는 JWT Access Token 기반이며, 서버에서 토큰을 별도로 저장하지 않습니다.
	 * 따라서 서버에서는 별도의 토큰 폐기 처리를 수행하지 않고, 클라이언트에서 보관 중인
	 * Access Token을 삭제하는 방식으로 로그아웃을 처리합니다.
	 */
	public void logout() {
		// JWT Access Token만 사용하는 구조에서는 서버에서 처리할 로직이 없습니다.
	}
}