package com.commercepaymentsystem.domain.membership.entity;

import com.commercepaymentsystem.global.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Entity
@Getter
@Table(name = "membership_grades")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class MembershipGrade extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NonNull
	@Column(nullable = false, unique = true, length = 30)
	private String name;

	@NonNull
	@Column(name = "min_cumulative_payment_amount", nullable = false)
	private Long minCumulativePaymentAmount;

	@NonNull
	@Column(name = "point_reward_rate", nullable = false)
	private Integer pointRewardRate;

	public static MembershipGrade create(
		String name,
		Long minCumulativePaymentAmount,
		Integer pointRewardRate
	) {
		return new MembershipGrade(
			name,
			minCumulativePaymentAmount,
			pointRewardRate
		);
	}
}