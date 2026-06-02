package com.commercepaymentsystem.domain.product.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.commercepaymentsystem.domain.product.entity.Product;

import jakarta.persistence.LockModeType;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Product p where p.id in :productIds")
	List<Product> findAllByIdInForUpdate(List<Long> productIds);
}
