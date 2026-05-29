package com.commercepaymentsystem.global.jwt;

import java.util.Base64;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtProvider {

	private final SecretKey key;
	private final long expiration;
	private final JwtParser parser;

	public JwtProvider(
		@Value("${jwt.secret}") String secret,
		@Value("${jwt.expiration}") long expiration) {
		this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
		this.expiration = expiration;
		this.parser = Jwts.parser()
			.verifyWith(this.key)
			.build();
	}

	public String createToken(Long memberId, String email) {
		Date now = new Date();
		return Jwts.builder()
			.subject(memberId.toString())
			.claim("email", email)
			.issuedAt(now)
			.expiration(new Date(now.getTime() + expiration))
			.signWith(key)
			.compact();
	}

	public Long getMemberId(String token) {
		return Long.parseLong(parseClaims(token).getSubject());
	}

	public boolean validate(String token) {
		try {
			parseClaims(token);
			return true;
		} catch (JwtException | IllegalArgumentException e) {
			log.warn("JWT 검증 실패: {}", e.getMessage());
			return false;
		}
	}

	private Claims parseClaims(String token) {
		return parser
			.parseSignedClaims(token)
			.getPayload();
	}

}
