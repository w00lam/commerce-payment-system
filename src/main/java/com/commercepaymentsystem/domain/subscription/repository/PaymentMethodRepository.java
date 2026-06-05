package com.commercepaymentsystem.domain.subscription.repository;

import com.commercepaymentsystem.domain.subscription.entity.PaymentMethod;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {
	List<PaymentMethod> findAllByMemberId(Long memberId);
	Optional<PaymentMethod> findByPortoneBillingKey(String portoneBillingKey);
}
