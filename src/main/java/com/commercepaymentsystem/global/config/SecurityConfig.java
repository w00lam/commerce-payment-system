package com.commercepaymentsystem.global.config;

import java.io.IOException;

import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.commercepaymentsystem.global.exception.GlobalErrorCode;
import com.commercepaymentsystem.global.filter.JwtAuthFilter;
import com.commercepaymentsystem.global.response.ApiResponse;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private static final String[] PUBLIC_PAGE_URLS = {
		"/",
		"/login",
		"/signup",
		"/products/**",
		"/cart",
		"/orders/**",
		"/checkout",
		"/favicon.*",
		"/error"
	};

	private static final String[] PUBLIC_API_URLS = {
		"/api/auth/signup",
		"/api/auth/login",
		"/api/products/**",
		"/api/webhooks/**",
		"/api/payments/webhooks/portone",
		"/api/config/**"
	};

	private final JwtAuthFilter jwtAuthFilter;
	private final ObjectMapper objectMapper;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			.csrf(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.sessionManagement(session ->
				session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.exceptionHandling(exception -> exception
				.authenticationEntryPoint((request, response, authException) ->
					writeErrorResponse(
						response,
						HttpServletResponse.SC_UNAUTHORIZED,
						GlobalErrorCode.UNAUTHORIZED
					)
				)
				.accessDeniedHandler((request, response, accessDeniedException) ->
					writeErrorResponse(
						response,
						HttpServletResponse.SC_FORBIDDEN,
						GlobalErrorCode.ACCESS_DENIED
					)
				)
			)
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
				.requestMatchers(PUBLIC_PAGE_URLS).permitAll()
				.requestMatchers(PUBLIC_API_URLS).permitAll()
				.anyRequest().authenticated()
			)
			.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	private void writeErrorResponse(
		HttpServletResponse response,
		int status,
		GlobalErrorCode errorCode
	) throws IOException {
		response.setStatus(status);
		response.setContentType("application/json;charset=UTF-8");
		response.getWriter().write(
			objectMapper.writeValueAsString(ApiResponse.error(errorCode))
		);
	}
}
