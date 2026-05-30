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

@Entity
@Getter
@Table(name = "members")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

	private static final long DEFAULT_POINT_BALANCE = 0L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(
		nullable = false,
		unique = true,
		length = 100
	)
	private String email;

	@Column(
		nullable = false,
		length = 255
	)
	private String password;

	@Column(
		nullable = false,
		length = 50
	)
	private String name;

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

	private Member(
		String email,
		String password,
		String name,
		String phone
	) {
		this.email = email;
		this.password = password;
		this.name = name;
		this.phone = phone;
		this.pointBalance = DEFAULT_POINT_BALANCE;
	}

	public static Member create(
		String email,
		String encodedPassword,
		String name,
		String phone
	) {
		return new Member(
			email,
			encodedPassword,
			name,
			phone
		);
	}
}