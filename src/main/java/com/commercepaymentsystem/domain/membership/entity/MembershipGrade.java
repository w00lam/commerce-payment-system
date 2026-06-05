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

@Entity
@Getter
@Table(name = "membership_grades")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MembershipGrade extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 50)
	private String name;

	@Column(name = "min_cumulative_payment_amount", nullable = false)
	private Long minCumulativePaymentAmount;

	@Column(name = "point_reward_rate", nullable = false)
	private Integer pointRewardRate;

	public MembershipGrade(String name, Long minCumulativePaymentAmount, Integer pointRewardRate) {
		this.name = name;
		this.minCumulativePaymentAmount = minCumulativePaymentAmount;
		this.pointRewardRate = pointRewardRate;
	}
}
