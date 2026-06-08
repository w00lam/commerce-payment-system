package com.commercepaymentsystem.domain.membership.dto;

import java.time.LocalDateTime;

import com.commercepaymentsystem.domain.membership.entity.MemberMembership;
import com.commercepaymentsystem.domain.membership.entity.MembershipGrade;

public record MembershipResponse(
	Long memberId,
	MembershipGradeSummaryResponse grade,
	Long cumulativePaymentAmount,
	NextMembershipGradeResponse nextGrade,
	LocalDateTime gradeUpdatedAt
) {
	public static MembershipResponse from(
		MemberMembership membership,
		MembershipGrade nextGrade
	) {
		return new MembershipResponse(
			membership.getMemberId(),
			MembershipGradeSummaryResponse.from(membership.getMembershipGrade()),
			membership.getCumulativePaymentAmount(),
			NextMembershipGradeResponse.from(
				nextGrade,
				membership.getCumulativePaymentAmount()
			),
			membership.getGradeUpdatedAt()
		);
	}
}