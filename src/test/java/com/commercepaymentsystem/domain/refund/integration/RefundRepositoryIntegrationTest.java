package com.commercepaymentsystem.domain.refund.integration;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.commercepaymentsystem.domain.refund.entity.Refund;
import com.commercepaymentsystem.domain.refund.entity.RefundItem;
import com.commercepaymentsystem.domain.refund.repository.RefundRepository;

import jakarta.persistence.EntityManager;

@ActiveProfiles("test")
@Transactional
@SpringBootTest
class RefundRepositoryIntegrationTest {

	@Autowired
	private RefundRepository refundRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	@DisplayName("Refund and items are saved and queried by id")
	void saveAndFindById_success() {
		Refund refund = refund();
		refund.addItem(RefundItem.create(10L, 2L, 1_000L, 5_000L));

		Refund savedRefund = refundRepository.saveAndFlush(refund);
		entityManager.clear();

		Refund found = refundRepository.findById(savedRefund.getId()).orElseThrow();

		assertThat(found.getPaymentId()).isEqualTo(100L);
		assertThat(found.getPointRefundAmount()).isEqualTo(2_000L);
		assertThat(found.getPgRefundAmount()).isEqualTo(8_000L);
		assertThat(found.getItems()).hasSize(1);
		assertThat(found.getItems().get(0).getPointRefundAmount()).isEqualTo(1_000L);
		assertThat(found.getItems().get(0).getPgRefundAmount()).isEqualTo(5_000L);
	}

	@Test
	@DisplayName("Refunds can be queried by payment id")
	void findByPaymentId_success() {
		refundRepository.saveAndFlush(refund());
		entityManager.clear();

		assertThat(refundRepository.findByPaymentId(100L)).hasSize(1);
	}

	@Test
	@DisplayName("Removing refund also removes refund items")
	void orphanRemoval_success() {
		Refund refund = refund();
		refund.addItem(RefundItem.create(10L, 1L, 500L, 2_500L));
		Refund savedRefund = refundRepository.saveAndFlush(refund);
		Long refundId = savedRefund.getId();

		refundRepository.delete(savedRefund);
		refundRepository.flush();
		entityManager.clear();

		assertThat(refundRepository.findById(refundId)).isEmpty();
	}

	private Refund refund() {
		return Refund.create(100L, "Customer request", 2_000L, 8_000L);
	}
}
