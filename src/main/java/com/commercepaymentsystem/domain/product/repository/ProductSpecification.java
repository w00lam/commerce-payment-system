package com.commercepaymentsystem.domain.product.repository;

import org.springframework.data.jpa.domain.Specification;

import com.commercepaymentsystem.domain.product.dto.ProductSearchCondition;
import com.commercepaymentsystem.domain.product.entity.Product;

/**
 * 다중 조건 검색을 위한 Spring Data JPA Specification 유틸리티 클래스입니다.
 * QueryDSL이 없는 환경에서 동적 쿼리(Dynamic Query)를 생성할 때 매우 유용합니다.
 */
public class ProductSpecification {

	/**
	 * 검색 조건(ProductSearchCondition)을 받아 조건에 맞는 Specification을 반환합니다.
	 * 값이 null인 조건은 쿼리에 포함되지 않습니다.
	 */
	public static Specification<Product> searchWith(ProductSearchCondition condition) {
		Specification<Product> spec = Specification.where((Specification<Product>) null);

		// 카테고리 필터: 정확히 일치(Equal)하는 경우
		if (condition.category() != null) {
			spec = spec.and((root, query, criteriaBuilder) ->
				criteriaBuilder.equal(root.get("category"), condition.category()));
		}

		// 최소 가격 필터: 크거나 같은(GreaterThanOrEqualTo) 경우
		if (condition.minPrice() != null) {
			spec = spec.and((root, query, criteriaBuilder) ->
				// root.<Long>get()을 사용하여 타입 추론 에러를 방지합니다.
				criteriaBuilder.greaterThanOrEqualTo(root.<Long>get("price"), condition.minPrice()));
		}

		// 최대 가격 필터: 작거나 같은(LessThanOrEqualTo) 경우
		if (condition.maxPrice() != null) {
			spec = spec.and((root, query, criteriaBuilder) ->
				criteriaBuilder.lessThanOrEqualTo(root.<Long>get("price"), condition.maxPrice()));
		}

		// 판매 상태 필터: 판매중, 품절 상태 정확히 일치(Equal)하는 경우
		if (condition.status() != null) {
			spec = spec.and((root, query, criteriaBuilder) ->
				criteriaBuilder.equal(root.get("status"), condition.status()));
		}

		return spec;
	}
}