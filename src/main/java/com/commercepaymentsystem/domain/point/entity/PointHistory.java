package com.commercepaymentsystem.domain.point.entity;

import com.commercepaymentsystem.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "point_histories")
public class PointHistory extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long memberId;

	private Long paymentId;

	@Enumerated(EnumType.STRING)
	private PointHistoryType type;

	private Long amount;

	public PointHistory(Long memberId, Long paymentId, PointHistoryType type, Long amount) {
		this.memberId = memberId;
		this.paymentId = paymentId;
		this.type = type;
		this.amount = amount;
	}
}
