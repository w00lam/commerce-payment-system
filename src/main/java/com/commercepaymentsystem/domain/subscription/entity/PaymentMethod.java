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
@Table(name = "payment_methods")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentMethod extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Column(name = "portone_billing_key", nullable = false, unique = true, length = 255)
	private String portoneBillingKey;

	@Column(name = "card_company_name", nullable = false, length = 100)
	private String cardCompanyName;

	public PaymentMethod(Long memberId, String portoneBillingKey, String cardCompanyName) {
		this.memberId = memberId;
		this.portoneBillingKey = portoneBillingKey;
		this.cardCompanyName = cardCompanyName;
	}
}
