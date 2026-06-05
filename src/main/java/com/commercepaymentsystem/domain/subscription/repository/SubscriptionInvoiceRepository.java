package com.commercepaymentsystem.domain.subscription.repository;

import com.commercepaymentsystem.domain.subscription.entity.SubscriptionInvoice;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionInvoiceRepository extends JpaRepository<SubscriptionInvoice, Long> {
	List<SubscriptionInvoice> findAllBySubscriptionId(Long subscriptionId);
	boolean existsBySubscriptionIdAndBillingPeriod(Long subscriptionId, String billingPeriod);
}
