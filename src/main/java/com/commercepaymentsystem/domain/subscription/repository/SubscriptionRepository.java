package com.commercepaymentsystem.domain.subscription.repository;

import com.commercepaymentsystem.domain.subscription.entity.Subscription;
import com.commercepaymentsystem.domain.subscription.entity.SubscriptionStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
	Optional<Subscription> findByMemberIdAndStatus(Long memberId, SubscriptionStatus status);

	List<Subscription> findAllByStatusAndNextBillingDateLessThanEqual(SubscriptionStatus status, LocalDate nextBillingDate);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select s from Subscription s where s.id = :id")
	Optional<Subscription> findByIdWithPessimisticLock(@Param("id") Long id);
}
