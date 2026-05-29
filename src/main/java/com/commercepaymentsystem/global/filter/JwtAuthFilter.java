package com.commercepaymentsystem.global.filter;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.commercepaymentsystem.global.exception.ErrorCode;
import com.commercepaymentsystem.global.jwt.JwtProvider;
import com.commercepaymentsystem.global.response.ApiResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

	private final JwtProvider jwtProvider;
	private final ObjectMapper objectMapper;

	@Override
	protected void doFilterInternal(HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain) throws ServletException, IOException {

		String header = request.getHeader("Authorization");

		if (header != null && header.startsWith("Bearer ")) {
			String token = header.substring(7);
			if (jwtProvider.validate(token)) {
				Long memberId = jwtProvider.getMemberId(token);
				UsernamePasswordAuthenticationToken auth =
					new UsernamePasswordAuthenticationToken(memberId, null, List.of());
				SecurityContextHolder.getContext().setAuthentication(auth);
			} else {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				response.setContentType("application/json;charset=UTF-8");
				response.getWriter().write(
					objectMapper.writeValueAsString(ApiResponse.error(ErrorCode.INVALID_TOKEN))
				);
				return;
			}
		}

		filterChain.doFilter(request, response);
	}
}
