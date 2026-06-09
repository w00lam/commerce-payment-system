package com.commercepaymentsystem.domain.membership.entity;

import java.time.LocalDateTime;

import com.commercepaymentsystem.domain.member.entity.Member;
import com.commercepaymentsystem.domain.membership.exception.MembershipErrorCode;
import com.commercepaymentsystem.global.entity.BaseEntity;
import com.commercepaymentsystem.global.exception.BusinessException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
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

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false, unique = true)
	private Member member;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "membership_grade_id", nullable = false)
	private MembershipGrade membershipGrade;

	@Column(name = "cumulative_payment_amount", nullable = false)
	private Long cumulativePaymentAmount;

	@Column(name = "grade_updated_at")
	private LocalDateTime gradeUpdatedAt;

	public static MemberMembership create(
		Member member,
		MembershipGrade membershipGrade
	) {
		MemberMembership memberMembership = new MemberMembership(
			member,
			membershipGrade
		);

		memberMembership.cumulativePaymentAmount = 0L;
		memberMembership.gradeUpdatedAt = LocalDateTime.now();

		return memberMembership;
	}

	public Long getMemberId() {
		return member.getId();
	}

	public void increaseCumulativePaymentAmount(long amount) {
		this.cumulativePaymentAmount += amount;
	}

	public void decreaseCumulativePaymentAmount(long amount) {
		this.cumulativePaymentAmount = Math.max(
			0L,
			this.cumulativePaymentAmount - amount
		);
	}

	public void changeGrade(MembershipGrade membershipGrade) {
		if (this.membershipGrade.getId().equals(membershipGrade.getId())) {
			return;
		}

		this.membershipGrade = membershipGrade;
		this.gradeUpdatedAt = LocalDateTime.now();
	}

	public void updateCumulativePaymentAmount(Long amount) {
		if (amount == null || amount < 0) {
			throw new BusinessException(
				MembershipErrorCode.INVALID_CUMULATIVE_PAYMENT_AMOUNT
			);
		}

		this.cumulativePaymentAmount = amount;
	}

	private MemberMembership(Member member, MembershipGrade membershipGrade) {
		this.member = member;
		this.membershipGrade = membershipGrade;
	}
}
