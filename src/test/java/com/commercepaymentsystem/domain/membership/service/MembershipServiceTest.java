package com.commercepaymentsystem.domain.membership.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.commercepaymentsystem.domain.member.entity.Member;
import com.commercepaymentsystem.domain.membership.dto.MembershipGradeResponse;
import com.commercepaymentsystem.domain.membership.dto.MembershipRecalculateResponse;
import com.commercepaymentsystem.domain.membership.dto.MembershipResponse;
import com.commercepaymentsystem.domain.membership.entity.MemberMembership;
import com.commercepaymentsystem.domain.membership.entity.MembershipGrade;
import com.commercepaymentsystem.domain.membership.exception.MembershipErrorCode;
import com.commercepaymentsystem.domain.membership.repository.MemberMembershipRepository;
import com.commercepaymentsystem.domain.membership.repository.MembershipGradeRepository;
import com.commercepaymentsystem.domain.payment.repository.PaymentRepository;
import com.commercepaymentsystem.domain.refund.repository.RefundRepository;
import com.commercepaymentsystem.global.exception.BusinessException;

class MembershipServiceTest {

	private final MemberMembershipRepository memberMembershipRepository =
		mock(MemberMembershipRepository.class);
	private final MembershipGradeRepository membershipGradeRepository =
		mock(MembershipGradeRepository.class);
	private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
	private final RefundRepository refundRepository = mock(RefundRepository.class);

	private final MembershipService membershipService = new MembershipService(
		memberMembershipRepository,
		membershipGradeRepository,
		paymentRepository,
		refundRepository
	);

	@Test
	@DisplayName("결제 금액을 누적하고 기준 금액에 맞는 등급으로 갱신한다")
	void applyPayment_increasesAmountAndChangesGrade() {
		MembershipGrade normal = grade(1L, "NORMAL", 0L, 1);
		MembershipGrade vip = grade(2L, "VIP", 50_000L, 5);
		MemberMembership membership = membership(normal, 0L);
		when(memberMembershipRepository.findByMemberIdForUpdate(1L))
			.thenReturn(Optional.of(membership));
		when(membershipGradeRepository
			.findFirstByMinCumulativePaymentAmountLessThanEqualOrderByMinCumulativePaymentAmountDesc(50_000L))
			.thenReturn(Optional.of(vip));

		membershipService.applyPayment(1L, 50_000L);

		assertThat(membership.getCumulativePaymentAmount()).isEqualTo(50_000L);
		assertThat(membership.getMembershipGrade()).isEqualTo(vip);
	}

	@Test
	@DisplayName("환불 금액을 차감하고 기준 금액에 맞는 등급으로 갱신한다")
	void applyRefund_decreasesAmountAndChangesGrade() {
		MembershipGrade normal = grade(1L, "NORMAL", 0L, 1);
		MembershipGrade vip = grade(2L, "VIP", 50_000L, 5);
		MemberMembership membership = membership(vip, 60_000L);
		when(memberMembershipRepository.findByMemberIdForUpdate(1L))
			.thenReturn(Optional.of(membership));
		when(membershipGradeRepository
			.findFirstByMinCumulativePaymentAmountLessThanEqualOrderByMinCumulativePaymentAmountDesc(40_000L))
			.thenReturn(Optional.of(normal));

		membershipService.applyRefund(1L, 20_000L);

		assertThat(membership.getCumulativePaymentAmount()).isEqualTo(40_000L);
		assertThat(membership.getMembershipGrade()).isEqualTo(normal);
	}

	@Test
	@DisplayName("결제 금액이 null이면 멤버십 조회 전에 예외를 던진다")
	void applyPayment_nullAmount_throwsException() {
		assertThatThrownBy(() -> membershipService.applyPayment(1L, null))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(MembershipErrorCode.INVALID_CUMULATIVE_PAYMENT_AMOUNT);

		verifyNoInteractions(memberMembershipRepository, membershipGradeRepository);
	}

	@Test
	@DisplayName("환불 금액이 음수이면 멤버십 조회 전에 예외를 던진다")
	void applyRefund_negativeAmount_throwsException() {
		assertThatThrownBy(() -> membershipService.applyRefund(1L, -1L))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(MembershipErrorCode.INVALID_CUMULATIVE_PAYMENT_AMOUNT);

		verifyNoInteractions(memberMembershipRepository, membershipGradeRepository);
	}

	@Test
	@DisplayName("누적 결제액을 결제 합계와 환불 합계로 재계산하고 음수 결과는 0으로 보정한다")
	void recalculate_rebuildsAmountAndFloorsAtZero() {
		MembershipGrade normal = grade(1L, "NORMAL", 0L, 1);
		MembershipGrade vvip = grade(3L, "VVIP", 100_000L, 10);
		MemberMembership membership = membership(vvip, 100_000L);
		when(memberMembershipRepository.findByMemberIdForUpdate(1L))
			.thenReturn(Optional.of(membership));
		when(paymentRepository.sumConfirmedFinalPaymentAmountByMemberId(1L))
			.thenReturn(40_000L);
		when(refundRepository.sumCompletedRefundAmountByMemberId(1L))
			.thenReturn(80_000L);
		when(membershipGradeRepository
			.findFirstByMinCumulativePaymentAmountLessThanEqualOrderByMinCumulativePaymentAmountDesc(0L))
			.thenReturn(Optional.of(normal));

		MembershipRecalculateResponse response = membershipService.recalculate(1L);

		assertThat(response.before().gradeName()).isEqualTo("VVIP");
		assertThat(response.before().cumulativePaymentAmount()).isEqualTo(100_000L);
		assertThat(response.after().gradeName()).isEqualTo("NORMAL");
		assertThat(response.after().cumulativePaymentAmount()).isZero();
		assertThat(response.gradeChanged()).isTrue();
		assertThat(membership.getMembershipGrade()).isEqualTo(normal);
	}

	@Test
	@DisplayName("내 멤버십 조회 시 현재 등급과 다음 등급 정보를 반환한다")
	void getMyMembership_returnsCurrentAndNextGrade() {
		MembershipGrade normal = grade(1L, "NORMAL", 0L, 1);
		MembershipGrade vip = grade(2L, "VIP", 50_000L, 5);
		MemberMembership membership = membership(normal, 10_000L);
		when(memberMembershipRepository.findByMemberId(1L))
			.thenReturn(Optional.of(membership));
		when(membershipGradeRepository.findAllByOrderByMinCumulativePaymentAmountAsc())
			.thenReturn(List.of(normal, vip));

		MembershipResponse response = membershipService.getMyMembership(1L);

		assertThat(response.memberId()).isEqualTo(1L);
		assertThat(response.grade().name()).isEqualTo("NORMAL");
		assertThat(response.cumulativePaymentAmount()).isEqualTo(10_000L);
		assertThat(response.nextGrade().name()).isEqualTo("VIP");
		assertThat(response.nextGrade().remainingAmount()).isEqualTo(40_000L);
	}

	@Test
	@DisplayName("등급 목록을 최소 누적 결제액 오름차순 응답으로 변환한다")
	void getGrades_returnsGradeResponses() {
		MembershipGrade normal = grade(1L, "NORMAL", 0L, 1);
		MembershipGrade vip = grade(2L, "VIP", 50_000L, 5);
		when(membershipGradeRepository.findAllByOrderByMinCumulativePaymentAmountAsc())
			.thenReturn(List.of(normal, vip));

		List<MembershipGradeResponse> responses = membershipService.getGrades();

		assertThat(responses).extracting(MembershipGradeResponse::name)
			.containsExactly("NORMAL", "VIP");
	}

	private MemberMembership membership(MembershipGrade grade, Long cumulativePaymentAmount) {
		Member member = Member.create(
			"user@example.com",
			"encoded-password",
			"tester",
			"010-1234-5678"
		);
		ReflectionTestUtils.setField(member, "id", 1L);

		MemberMembership membership = MemberMembership.create(member, grade);
		membership.updateCumulativePaymentAmount(cumulativePaymentAmount);
		return membership;
	}

	private MembershipGrade grade(
		Long id,
		String name,
		Long minCumulativePaymentAmount,
		Integer pointRewardRate
	) {
		MembershipGrade grade = MembershipGrade.create(
			name,
			minCumulativePaymentAmount,
			pointRewardRate
		);
		ReflectionTestUtils.setField(grade, "id", id);
		return grade;
	}
}
