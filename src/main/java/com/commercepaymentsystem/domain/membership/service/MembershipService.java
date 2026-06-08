package com.commercepaymentsystem.domain.membership.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commercepaymentsystem.domain.membership.dto.MembershipGradeResponse;
import com.commercepaymentsystem.domain.membership.dto.MembershipRecalculateResponse;
import com.commercepaymentsystem.domain.membership.dto.MembershipRecalculateSnapshot;
import com.commercepaymentsystem.domain.membership.dto.MembershipResponse;
import com.commercepaymentsystem.domain.membership.entity.MemberMembership;
import com.commercepaymentsystem.domain.membership.entity.MembershipGrade;
import com.commercepaymentsystem.domain.membership.exception.MembershipErrorCode;
import com.commercepaymentsystem.domain.membership.repository.MemberMembershipRepository;
import com.commercepaymentsystem.domain.membership.repository.MembershipGradeRepository;
import com.commercepaymentsystem.domain.payment.repository.PaymentRepository;
import com.commercepaymentsystem.domain.refund.repository.RefundRepository;
import com.commercepaymentsystem.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MembershipService {

	private final MemberMembershipRepository memberMembershipRepository;
	private final MembershipGradeRepository membershipGradeRepository;
	private final PaymentRepository paymentRepository;
	private final RefundRepository refundRepository;

	public MembershipResponse getMyMembership(Long memberId) {
		MemberMembership membership = getMembership(memberId);
		MembershipGrade nextGrade = findNextGrade(
			membership.getCumulativePaymentAmount()
		);

		return MembershipResponse.from(membership, nextGrade);
	}

	public List<MembershipGradeResponse> getGrades() {
		return membershipGradeRepository.findAllByOrderByMinCumulativePaymentAmountAsc()
			.stream()
			.map(MembershipGradeResponse::from)
			.toList();
	}

	@Transactional
	public void applyPayment(
		Long memberId,
		Long paidAmount
	) {
		long validPaidAmount = validateMembershipAmount(paidAmount);
		MemberMembership membership = getMembershipForUpdate(memberId);

		membership.increaseCumulativePaymentAmount(validPaidAmount);
		membership.changeGrade(resolveGrade(membership.getCumulativePaymentAmount()));
	}

	@Transactional
	public void applyRefund(
		Long memberId,
		Long refundAmount
	) {
		long validRefundAmount = validateMembershipAmount(refundAmount);
		MemberMembership membership = getMembershipForUpdate(memberId);

		membership.decreaseCumulativePaymentAmount(validRefundAmount);
		membership.changeGrade(resolveGrade(membership.getCumulativePaymentAmount()));
	}

	@Transactional
	public MembershipRecalculateResponse recalculate(Long memberId) {
		MemberMembership membership = getMembershipForUpdate(memberId);

		MembershipRecalculateSnapshot before =
			MembershipRecalculateSnapshot.from(membership);

		Long recalculatedAmount = calculateCumulativePaymentAmount(memberId);
		membership.updateCumulativePaymentAmount(recalculatedAmount);

		MembershipGrade newGrade = resolveGrade(recalculatedAmount);
		membership.changeGrade(newGrade);

		MembershipRecalculateSnapshot after =
			MembershipRecalculateSnapshot.from(membership);

		boolean gradeChanged = !before.gradeName().equals(after.gradeName());

		return new MembershipRecalculateResponse(
			memberId,
			before,
			after,
			gradeChanged,
			membership.getGradeUpdatedAt()
		);
	}

	private MemberMembership getMembership(Long memberId) {
		return memberMembershipRepository.findByMemberId(memberId)
			.orElseThrow(() -> new BusinessException(
				MembershipErrorCode.MEMBERSHIP_NOT_FOUND
			));
	}

	private MemberMembership getMembershipForUpdate(Long memberId) {
		return memberMembershipRepository.findByMemberIdForUpdate(memberId)
			.orElseThrow(() -> new BusinessException(
				MembershipErrorCode.MEMBERSHIP_NOT_FOUND
			));
	}

	private MembershipGrade resolveGrade(Long cumulativePaymentAmount) {
		return membershipGradeRepository
			.findFirstByMinCumulativePaymentAmountLessThanEqualOrderByMinCumulativePaymentAmountDesc(
				cumulativePaymentAmount
			)
			.orElseThrow(() -> new BusinessException(
				MembershipErrorCode.INVALID_GRADE_POLICY
			));
	}

	private MembershipGrade findNextGrade(Long cumulativePaymentAmount) {
		return membershipGradeRepository.findAllByOrderByMinCumulativePaymentAmountAsc()
			.stream()
			.filter(grade -> grade.getMinCumulativePaymentAmount() > cumulativePaymentAmount)
			.findFirst()
			.orElse(null);
	}

	private Long calculateCumulativePaymentAmount(Long memberId) {
		Long confirmedPaymentAmount = paymentRepository.sumConfirmedFinalPaymentAmountByMemberId(memberId);
		Long completedRefundAmount = refundRepository.sumCompletedRefundAmountByMemberId(memberId);

		return Math.max(
			0L,
			nullToZero(confirmedPaymentAmount) - nullToZero(completedRefundAmount)
		);
	}

	private Long nullToZero(Long amount) {
		return amount == null ? 0L : amount;
	}

	private long validateMembershipAmount(Long amount) {
		if (amount == null || amount < 0) {
			throw new BusinessException(
				MembershipErrorCode.INVALID_CUMULATIVE_PAYMENT_AMOUNT
			);
		}

		return amount;
	}
}
