package com.commercepaymentsystem.domain.point.entity;

import com.commercepaymentsystem.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
	name = "point_histories",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_point_history_idempotency",
			columnNames = {"payment_id", "type", "refund_id"}
		)
	}
)
public class PointHistory extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long memberId;

	@Column(nullable = false)
	private Long paymentId;

	private Long refundId; // 부분 환불 시 식별을 위해 추가

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PointHistoryType type;

	@Column(nullable = false)
	private Long amount;

	public PointHistory(Long memberId, Long paymentId, PointHistoryType type, Long amount) {
		this(memberId, paymentId, null, type, amount);
	}

	public PointHistory(Long memberId, Long paymentId, Long refundId, PointHistoryType type, Long amount) {
		this.memberId = memberId;
		this.paymentId = paymentId;
		this.refundId = refundId;
		this.type = type;
		this.amount = amount;
	}
}
