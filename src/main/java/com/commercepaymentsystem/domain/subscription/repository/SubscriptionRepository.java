package com.commercepaymentsystem.domain.subscription.repository;

import com.commercepaymentsystem.domain.subscription.entity.Subscription;
import com.commercepaymentsystem.domain.subscription.entity.SubscriptionStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
	Optional<Subscription> findByMemberIdAndStatus(Long memberId, SubscriptionStatus status);

	Slice<Subscription> findAllByStatusAndNextBillingDateLessThanEqual(
		SubscriptionStatus status,
		LocalDate nextBillingDate,
		Pageable pageable
	);

	@Query("select s from Subscription s where s.status = :status and s.nextBillingDate <= :nextBillingDate and s.id > :lastId order by s.id asc")
	Slice<Subscription> findAllByStatusAndNextBillingDateLessThanEqualAndIdGreaterThanOrderByIdAsc(
		@Param("status") SubscriptionStatus status,
		@Param("nextBillingDate") LocalDate nextBillingDate,
		@Param("lastId") Long lastId,
		Pageable pageable
	);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select s from Subscription s where s.id = :id")
	Optional<Subscription> findByIdWithPessimisticLock(@Param("id") Long id);
}
