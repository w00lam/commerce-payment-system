package com.commercepaymentsystem.domain.membership.dto;

import com.commercepaymentsystem.domain.membership.entity.MembershipGrade;

public record MembershipGradeSummaryResponse(
	Long id,
	String name,
	Integer pointRewardRate
) {
	public static MembershipGradeSummaryResponse from(MembershipGrade grade) {
		return new MembershipGradeSummaryResponse(
			grade.getId(),
			grade.getName(),
			grade.getPointRewardRate()
		);
	}
}