package com.commercepaymentsystem.domain.product.repository;

import org.springframework.data.jpa.domain.Specification;

import com.commercepaymentsystem.domain.product.dto.ProductSearchCondition;
import com.commercepaymentsystem.domain.product.entity.Product;

public class ProductSpecification {

	public static Specification<Product> searchWith(ProductSearchCondition condition) {
		Specification<Product> spec = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();

		if (condition.category() != null) {
			spec = spec.and((root, query, criteriaBuilder) ->
				criteriaBuilder.equal(root.get("category"), condition.category()));
		}

		if (condition.status() != null) {
			spec = spec.and((root, query, criteriaBuilder) ->
				criteriaBuilder.equal(root.get("status"), condition.status()));
		}

		return spec;
	}
}