package com.commercepaymentsystem.domain.membership.entity;

import com.commercepaymentsystem.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "member_memberships")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberMembership extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "member_id", nullable = false, unique = true)
	private Long memberId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "membership_grade_id", nullable = false)
	private MembershipGrade membershipGrade;

	@Column(name = "cumulative_payment_amount", nullable = false)
	private Long cumulativePaymentAmount;

	@Column(name = "grade_updated_at")
	private LocalDateTime gradeUpdatedAt;

	public MemberMembership(Long memberId, MembershipGrade membershipGrade, Long cumulativePaymentAmount) {
		this.memberId = memberId;
		this.membershipGrade = membershipGrade;
		this.cumulativePaymentAmount = cumulativePaymentAmount;
		this.gradeUpdatedAt = LocalDateTime.now();
	}
}
