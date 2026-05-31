package com.commercepaymentsystem.domain.member.entity;

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
}