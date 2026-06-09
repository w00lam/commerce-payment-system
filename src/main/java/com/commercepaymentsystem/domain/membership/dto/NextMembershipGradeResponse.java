package com.commercepaymentsystem.domain.membership.dto;

import com.commercepaymentsystem.domain.membership.entity.MembershipGrade;

public record NextMembershipGradeResponse(
	Long id,
	String name,
	Long minCumulativePaymentAmount,
	Long remainingAmount,
	Integer pointRewardRate
) {
	public static NextMembershipGradeResponse from(
		MembershipGrade nextGrade,
		Long cumulativePaymentAmount
	) {
		if (nextGrade == null) {
			return null;
		}

		return new NextMembershipGradeResponse(
			nextGrade.getId(),
			nextGrade.getName(),
			nextGrade.getMinCumulativePaymentAmount(),
			nextGrade.getMinCumulativePaymentAmount() - cumulativePaymentAmount,
			nextGrade.getPointRewardRate()
		);
	}
}