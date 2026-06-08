package com.commercepaymentsystem.domain.membership.dto;

import com.commercepaymentsystem.domain.membership.entity.MemberMembership;
import com.commercepaymentsystem.domain.membership.entity.MembershipGrade;

public record MembershipRecalculateSnapshot(
	String gradeName,
	Long cumulativePaymentAmount,
	Integer pointRewardRate
) {
	public static MembershipRecalculateSnapshot from(MemberMembership membership) {
		MembershipGrade grade = membership.getMembershipGrade();

		return new MembershipRecalculateSnapshot(
			grade.getName(),
			membership.getCumulativePaymentAmount(),
			grade.getPointRewardRate()
		);
	}
}