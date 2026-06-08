package com.commercepaymentsystem.domain.membership.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.commercepaymentsystem.domain.membership.dto.MembershipGradeResponse;
import com.commercepaymentsystem.domain.membership.dto.MembershipGradeSummaryResponse;
import com.commercepaymentsystem.domain.membership.dto.MembershipRecalculateResponse;
import com.commercepaymentsystem.domain.membership.dto.MembershipRecalculateSnapshot;
import com.commercepaymentsystem.domain.membership.dto.MembershipResponse;
import com.commercepaymentsystem.domain.membership.dto.NextMembershipGradeResponse;
import com.commercepaymentsystem.domain.membership.service.MembershipService;
import com.commercepaymentsystem.global.response.ApiResponse;

class MembershipControllerTest {

	private final MembershipService membershipService = mock(MembershipService.class);
	private final MembershipController membershipController =
		new MembershipController(membershipService);

	@Test
	@DisplayName("내 멤버십 조회 요청은 성공 응답으로 멤버십 정보를 반환한다")
	void getMyMembership_success() {
		LocalDateTime gradeUpdatedAt = LocalDateTime.parse("2026-06-08T10:00:00");
		MembershipResponse membershipResponse = new MembershipResponse(
			1L,
			new MembershipGradeSummaryResponse(1L, "NORMAL", 1),
			10_000L,
			new NextMembershipGradeResponse(2L, "VIP", 50_000L, 40_000L, 5),
			gradeUpdatedAt
		);
		when(membershipService.getMyMembership(1L)).thenReturn(membershipResponse);

		ApiResponse<MembershipResponse> response =
			membershipController.getMyMembership(1L);

		assertThat(response.getCode()).isEqualTo("SUCCESS");
		assertThat(response.getData().memberId()).isEqualTo(1L);
		assertThat(response.getData().grade().name()).isEqualTo("NORMAL");
		verify(membershipService).getMyMembership(1L);
	}

	@Test
	@DisplayName("멤버십 등급 목록 조회 요청은 성공 응답으로 등급 목록을 반환한다")
	void getGrades_success() {
		when(membershipService.getGrades()).thenReturn(List.of(
			new MembershipGradeResponse(1L, "NORMAL", 0L, 1),
			new MembershipGradeResponse(2L, "VIP", 50_000L, 5)
		));

		ApiResponse<List<MembershipGradeResponse>> response =
			membershipController.getGrades();

		assertThat(response.getCode()).isEqualTo("SUCCESS");
		assertThat(response.getData()).hasSize(2);
		assertThat(response.getData()).extracting(MembershipGradeResponse::name)
			.containsExactly("NORMAL", "VIP");
		verify(membershipService).getGrades();
	}

	@Test
	@DisplayName("멤버십 재계산 요청은 성공 응답으로 재계산 전후 스냅샷을 반환한다")
	void recalculate_success() {
		LocalDateTime gradeUpdatedAt = LocalDateTime.parse("2026-06-08T10:00:00");
		MembershipRecalculateResponse recalculateResponse =
			new MembershipRecalculateResponse(
				1L,
				new MembershipRecalculateSnapshot("VIP", 50_000L, 5),
				new MembershipRecalculateSnapshot("NORMAL", 0L, 1),
				true,
				gradeUpdatedAt
			);
		when(membershipService.recalculate(1L)).thenReturn(recalculateResponse);

		ApiResponse<MembershipRecalculateResponse> response =
			membershipController.recalculate(1L);

		assertThat(response.getCode()).isEqualTo("SUCCESS");
		assertThat(response.getData().memberId()).isEqualTo(1L);
		assertThat(response.getData().gradeChanged()).isTrue();
		assertThat(response.getData().after().gradeName()).isEqualTo("NORMAL");
		verify(membershipService).recalculate(1L);
	}
}
