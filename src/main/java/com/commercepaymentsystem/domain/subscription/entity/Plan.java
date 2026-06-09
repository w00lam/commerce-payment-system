package com.commercepaymentsystem.domain.subscription.entity;

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
@Table(name = "plans")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Plan extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 100)
	private String name;

	@Column(name = "monthly_amount", nullable = false)
	private Long monthlyAmount;

	@Column(length = 255)
	private String description;

	public Plan(String name, Long monthlyAmount, String description) {
		this.name = name;
		this.monthlyAmount = monthlyAmount;
		this.description = description;
	}
}
