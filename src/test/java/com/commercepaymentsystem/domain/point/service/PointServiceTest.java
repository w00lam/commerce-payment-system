package com.commercepaymentsystem.domain.point.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.commercepaymentsystem.domain.member.entity.Member;
import com.commercepaymentsystem.domain.member.repository.MemberRepository;
import com.commercepaymentsystem.domain.point.dto.PointHistoryResponse;
import com.commercepaymentsystem.domain.point.dto.PointResponse;
import com.commercepaymentsystem.domain.point.entity.PointHistory;
import com.commercepaymentsystem.domain.point.entity.PointHistoryType;
import com.commercepaymentsystem.domain.point.repository.PointHistoryRepository;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

	@InjectMocks
	private PointService pointService;

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private PointHistoryRepository pointHistoryRepository;

	@Test
	@DisplayName("현재 포인트 잔액을 성공적으로 조회한다.")
	void getMyPoint_Success() {
		// given
		Long memberId = 1L;
		Member member = mock(Member.class);
		given(member.getPointBalance()).willReturn(1000L); // 이 부분에서 컴파일 에러 발생 가능 (Member에 메서드 없을 시)
		given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

		// when
		PointResponse response = pointService.getMyPoint(memberId);

		// then
		assertThat(response.pointBalance()).isEqualTo(1000L);
		verify(memberRepository).findById(memberId);
	}

	@Test
	@DisplayName("포인트 거래 내역을 최신순으로 조회한다.")
	void getMyPointHistories_Success() {
		// given
		Long memberId = 1L;
		PointHistory history1 = mock(PointHistory.class);
		PointHistory history2 = mock(PointHistory.class);

		given(history1.getType()).willReturn(PointHistoryType.EARN);
		given(history1.getAmount()).willReturn(500L);
		given(history2.getType()).willReturn(PointHistoryType.USE);
		given(history2.getAmount()).willReturn(200L);

		given(pointHistoryRepository.findByMemberIdOrderByCreatedAtDesc(memberId))
			.willReturn(List.of(history1, history2));

		// when
		List<PointHistoryResponse> responses = pointService.getMyPointHistories(memberId);

		// then
		assertThat(responses).hasSize(2);
		assertThat(responses.get(0).type()).isEqualTo("EARN");
		assertThat(responses.get(0).amount()).isEqualTo(500L);
		assertThat(responses.get(1).type()).isEqualTo("USE");
		assertThat(responses.get(1).amount()).isEqualTo(200L);
		verify(pointHistoryRepository).findByMemberIdOrderByCreatedAtDesc(memberId);
	}

	@Test
	@DisplayName("존재하지 않는 회원의 포인트를 조회하면 예외가 발생한다.")
	void getMyPoint_MemberNotFound() {
		// given
		Long memberId = 1L;
		given(memberRepository.findById(memberId)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> pointService.getMyPoint(memberId))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("회원을 찾을 수 없습니다.");
	}
}
