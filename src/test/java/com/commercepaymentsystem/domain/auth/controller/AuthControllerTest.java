package com.commercepaymentsystem.domain.auth.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.commercepaymentsystem.domain.auth.service.AuthService;

class AuthControllerTest {

	private AuthService authService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		authService = Mockito.mock(AuthService.class);

		mockMvc = MockMvcBuilders
			.standaloneSetup(new AuthController(authService))
			.build();
	}

	@Test
	@DisplayName("로그아웃 요청 시 성공 응답을 반환한다")
	void logout_success() throws Exception {
		mockMvc.perform(post("/api/auth/logout"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("SUCCESS"))
			.andExpect(jsonPath("$.message").value("요청 성공"))
			.andExpect(jsonPath("$.data").doesNotExist());

		verify(authService).logout();
	}
}