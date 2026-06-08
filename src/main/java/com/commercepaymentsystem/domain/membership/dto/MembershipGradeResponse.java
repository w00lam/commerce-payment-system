package com.commercepaymentsystem.domain.membership.dto;

import com.commercepaymentsystem.domain.membership.entity.MembershipGrade;

public record MembershipGradeResponse(
	Long id,
	String name,
	Long minCumulativePaymentAmount,
	Integer pointRewardRate
) {
	public static MembershipGradeResponse from(MembershipGrade grade) {
		return new MembershipGradeResponse(
			grade.getId(),
			grade.getName(),
			grade.getMinCumulativePaymentAmount(),
			grade.getPointRewardRate()
		);
	}
}