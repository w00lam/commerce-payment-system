package com.commercepaymentsystem.domain.membership.dto;

import java.time.LocalDateTime;

public record MembershipRecalculateResponse(
	Long memberId,
	MembershipRecalculateSnapshot before,
	MembershipRecalculateSnapshot after,
	Boolean gradeChanged,
	LocalDateTime gradeUpdatedAt
) {
}