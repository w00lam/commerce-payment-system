package com.commercepaymentsystem.domain.member.entity;

import java.time.LocalDateTime;

import com.commercepaymentsystem.domain.point.exception.PointErrorCode;
import com.commercepaymentsystem.global.entity.BaseEntity;
import com.commercepaymentsystem.global.exception.BusinessException;
import com.commercepaymentsystem.domain.member.exception.MemberErrorCode;
import com.commercepaymentsystem.global.exception.BusinessException;
import com.commercepaymentsystem.global.exception.GlobalErrorCode;

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
@Table(name = "members")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Member extends BaseEntity {

	private static final long DEFAULT_POINT_BALANCE = 0L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NonNull
	@Column(
		nullable = false,
		unique = true,
		length = 100
	)
	private String email;

	@NonNull
	@Column(
		nullable = false,
		length = 255
	)
	private String password;

	@NonNull
	@Column(
		nullable = false,
		length = 50
	)
	private String name;

	@NonNull
	@Column(
		nullable = false,
		length = 20
	)
	private String phone;

	@Column(
		name = "point_balance",
		nullable = false
	)
	private Long pointBalance;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	public static Member create(
		String email,
		String encodedPassword,
		String name,
		String phone
	) {
		Member member = new Member(
			email,
			encodedPassword,
			name,
			phone
		);

		member.pointBalance = DEFAULT_POINT_BALANCE;

		return member;
	}

	public void delete() {
		this.deletedAt = LocalDateTime.now();
	}

	public boolean isDeleted() {
		return this.deletedAt != null;
	}

	public void addPoint(Long amount) {
		validateAmount(amount);
		this.pointBalance += amount;
	}

	public void deductPoint(Long amount) {
		validateAmount(amount);
		if (this.pointBalance < amount) {
			throw new BusinessException(MemberErrorCode.POINT_NOT_ENOUGH);
		}
		this.pointBalance -= amount;
	}

	private void validateAmount(Long amount) {
		if (amount == null || amount <= 0) {
			throw new BusinessException(GlobalErrorCode.INVALID_INPUT_VALUE);
		}
	}

	public void usePoint(Long amount) {
		if (amount == null || amount <= 0) {
			throw new BusinessException(PointErrorCode.INVALID_POINT_AMOUNT);
		}

		if (this.pointBalance < amount) {
			throw new BusinessException(PointErrorCode.INSUFFICIENT_POINT);
		}

		this.pointBalance -= amount;
	}
}
