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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.commercepaymentsystem.domain.member.entity.Member;
import com.commercepaymentsystem.domain.member.exception.MemberErrorCode;
import com.commercepaymentsystem.domain.member.repository.MemberRepository;
import com.commercepaymentsystem.domain.point.dto.PointHistoryResponse;
import com.commercepaymentsystem.domain.point.dto.PointResponse;
import com.commercepaymentsystem.domain.point.entity.PointHistory;
import com.commercepaymentsystem.domain.point.entity.PointHistoryType;
import com.commercepaymentsystem.domain.point.exception.PointErrorCode;
import com.commercepaymentsystem.domain.point.exception.PointException;
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
		given(member.getPointBalance()).willReturn(1000L);
		given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

		// when
		PointResponse response = pointService.getMyPoint(memberId);

		// then
		assertThat(response.pointBalance()).isEqualTo(1000L);
		verify(memberRepository).findById(memberId);
	}

	@Test
	@DisplayName("포인트를 성공적으로 적립하고 이력을 남긴다.")
	void earnPoint_Success() {
		// given
		Long memberId = 1L;
		Long amount = 500L;
		Long paymentId = 100L;
		Member member = Member.create("test@test.com", "pw", "name", "01012345678");
		
		given(memberRepository.findByIdWithPessimisticLock(memberId)).willReturn(Optional.of(member));
		given(pointHistoryRepository.existsByPaymentIdAndType(paymentId, PointHistoryType.EARN)).willReturn(false);

		// when
		pointService.earnPoint(memberId, amount, paymentId);

		// then
		assertThat(member.getPointBalance()).isEqualTo(500L);
		verify(pointHistoryRepository).save(any(PointHistory.class));
	}

	@Test
	@DisplayName("이미 적립된 결제 건에 대해 다시 적립을 시도하면 추가 적립 없이 리턴한다.")
	void earnPoint_AlreadyEarned() {
		// given
		Long memberId = 1L;
		Long amount = 500L;
		Long paymentId = 100L;
		Member member = mock(Member.class);

		given(memberRepository.findByIdWithPessimisticLock(memberId)).willReturn(Optional.of(member));
		given(pointHistoryRepository.existsByPaymentIdAndType(paymentId, PointHistoryType.EARN)).willReturn(true);

		// when
		pointService.earnPoint(memberId, amount, paymentId);

		// then
		verify(member, never()).addPoint(anyLong());
		verify(pointHistoryRepository, never()).save(any(PointHistory.class));
	}

	@Test
	@DisplayName("적립 금액이 0원 이하이면 PointException이 발생한다.")
	void earnPoint_InvalidAmount() {
		// given
		Long memberId = 1L;
		Long amount = 0L;
		Long paymentId = 100L;

		// when & then
		assertThatThrownBy(() -> pointService.earnPoint(memberId, amount, paymentId))
			.isInstanceOf(PointException.class)
			.extracting("errorCode")
			.isEqualTo(PointErrorCode.INVALID_POINT_AMOUNT);
	}

	@Test
	@DisplayName("결제 식별자(paymentId)가 없으면 예외가 발생한다.")
	void earnPoint_NullPaymentId() {
		// given
		Long memberId = 1L;
		Long amount = 500L;
		Long paymentId = null;

		// when & then
		assertThatThrownBy(() -> pointService.earnPoint(memberId, amount, paymentId))
			.isInstanceOf(PointException.class)
			.extracting("errorCode")
			.isEqualTo(PointErrorCode.PAYMENT_ID_REQUIRED);
	}

	@Test
	@DisplayName("포인트를 성공적으로 차감하고 이력을 남긴다.")
	void deductPoint_Success() {
		// given
		Long memberId = 1L;
		Long amount = 500L;
		Long paymentId = 100L;
		Member member = Member.create("test@test.com", "pw", "name", "01012345678");
		member.addPoint(1000L); // 초기 잔액 1000원

		given(memberRepository.findByIdWithPessimisticLock(memberId)).willReturn(Optional.of(member));
		given(pointHistoryRepository.existsByPaymentIdAndType(paymentId, PointHistoryType.USE)).willReturn(false);

		// when
		pointService.deductPoint(memberId, amount, paymentId);

		// then
		assertThat(member.getPointBalance()).isEqualTo(500L);
		verify(pointHistoryRepository).save(any(PointHistory.class));
	}

	@Test
	@DisplayName("보유 포인트가 부족하면 PointException이 발생한다.")
	void deductPoint_InsufficientPoint() {
		// given
		Long memberId = 1L;
		Long amount = 1500L;
		Long paymentId = 100L;
		Member member = Member.create("test@test.com", "pw", "name", "01012345678");
		member.addPoint(1000L); // 초기 잔액 1000원

		given(memberRepository.findByIdWithPessimisticLock(memberId)).willReturn(Optional.of(member));
		given(pointHistoryRepository.existsByPaymentIdAndType(paymentId, PointHistoryType.USE)).willReturn(false);

		// when & then
		assertThatThrownBy(() -> pointService.deductPoint(memberId, amount, paymentId))
			.isInstanceOf(PointException.class)
			.extracting("errorCode")
			.isEqualTo(PointErrorCode.INSUFFICIENT_POINT);
	}

	@Test
	@DisplayName("이미 차감된 결제 건에 대해 다시 차감을 시도하면 추가 차감 없이 리턴한다.")
	void deductPoint_AlreadyDeducted() {
		// given
		Long memberId = 1L;
		Long amount = 500L;
		Long paymentId = 100L;
		Member member = mock(Member.class);

		given(memberRepository.findByIdWithPessimisticLock(memberId)).willReturn(Optional.of(member));
		given(pointHistoryRepository.existsByPaymentIdAndType(paymentId, PointHistoryType.USE)).willReturn(true);

		// when
		pointService.deductPoint(memberId, amount, paymentId);

		// then
		verify(member, never()).deductPoint(anyLong());
		verify(pointHistoryRepository, never()).save(any(PointHistory.class));
	}

	@Test
	@DisplayName("포인트 거래 내역을 최신순으로 페이징 조회한다.")
	void getMyPointHistories_Success() {
		// given
		Long memberId = 1L;
		Pageable pageable = PageRequest.of(0, 20);
		PointHistory history1 = mock(PointHistory.class);
		PointHistory history2 = mock(PointHistory.class);

		given(history1.getType()).willReturn(PointHistoryType.EARN);
		given(history1.getAmount()).willReturn(500L);
		given(history2.getType()).willReturn(PointHistoryType.EARN);
		given(history2.getAmount()).willReturn(200L);

		given(memberRepository.existsById(memberId)).willReturn(true);
		Page<PointHistory> page = new PageImpl<>(List.of(history1, history2), pageable, 2);
		given(pointHistoryRepository.findByMemberId(memberId, pageable)).willReturn(page);

		// when
		Page<PointHistoryResponse> responses = pointService.getMyPointHistories(memberId, pageable);

		// then
		assertThat(responses.getContent()).hasSize(2);
		verify(pointHistoryRepository).findByMemberId(memberId, pageable);
	}

	@Test
	@DisplayName("존재하지 않는 회원의 포인트를 조회하면 PointException이 발생한다.")
	void getMyPoint_MemberNotFound() {
		// given
		Long memberId = 1L;
		given(memberRepository.findById(memberId)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> pointService.getMyPoint(memberId))
			.isInstanceOf(PointException.class)
			.extracting("errorCode")
			.isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND);
	}

	@Test
	@DisplayName("존재하지 않는 회원의 포인트 이력을 조회하면 PointException이 발생한다.")
	void getMyPointHistories_MemberNotFound() {
		// given
		Long memberId = 1L;
		Pageable pageable = PageRequest.of(0, 20);
		given(memberRepository.existsById(memberId)).willReturn(false);

		// when & then
		assertThatThrownBy(() -> pointService.getMyPointHistories(memberId, pageable))
			.isInstanceOf(PointException.class)
			.extracting("errorCode")
			.isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND);
	}
}
