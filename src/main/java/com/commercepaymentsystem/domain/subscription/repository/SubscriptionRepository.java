package com.commercepaymentsystem.domain.subscription.repository;

import com.commercepaymentsystem.domain.subscription.entity.Subscription;
import com.commercepaymentsystem.domain.subscription.entity.SubscriptionStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
	Optional<Subscription> findByMemberIdAndStatus(Long memberId, SubscriptionStatus status);
	List<Subscription> findAllByStatusAndNextBillingDate(SubscriptionStatus status, LocalDate nextBillingDate);
}
