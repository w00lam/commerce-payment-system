package com.commercepaymentsystem.domain.product.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commercepaymentsystem.domain.product.dto.ProductDetailResponse;
import com.commercepaymentsystem.domain.product.dto.ProductListResponse;
import com.commercepaymentsystem.domain.product.dto.ProductSearchCondition;
import com.commercepaymentsystem.domain.product.entity.Product;
import com.commercepaymentsystem.domain.product.exception.ProductErrorCode;
import com.commercepaymentsystem.domain.product.repository.ProductRepository;
import com.commercepaymentsystem.domain.product.repository.ProductSpecification;
import com.commercepaymentsystem.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * 상품 도메인의 비즈니스 로직을 담당하는 서비스 클래스입니다.
 * 컨벤션에 따라 @Transactional(readOnly = true)를 기본으로 적용하여 조회 성능을 최적화합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

	private final ProductRepository productRepository;

	/**
	 * 다중 조건(카테고리, 가격 등)과 페이징 정보를 받아 상품 목록을 조회합니다.
	 *
	 * @param condition 동적 검색 조건 DTO
	 * @param pageable  페이징 및 정렬 객체
	 * @return Page 형태의 상품 목록 응답 DTO
	 */
	public Page<ProductListResponse> getProducts(ProductSearchCondition condition, Pageable pageable) {
		// Specification 유틸리티를 사용해 동적 조건이 적용된 쿼리로 엔티티를 페이징 조회합니다.
		Page<Product> productPage = productRepository.findAll(
			ProductSpecification.searchWith(condition),
			pageable
		);

		// 엔티티를 외부로 반환하지 않기 위해 DTO의 from() 메서드를 사용해 변환(map)합니다.
		return productPage.map(ProductListResponse::from);
	}

	/**
	 * 상품 ID를 받아 단건 상세 정보를 조회합니다.
	 *
	 * @param productId 조회할 상품의 고유 ID
	 * @return 상품 상세 정보 응답 DTO
	 * @throws BusinessException 상품이 존재하지 않을 경우 도메인 전용 에러(PRODUCT_NOT_FOUND) 발생
	 */
	public ProductDetailResponse getProductDetail(Long productId) {
		Product product = productRepository.findById(productId)
			// Optional로 반환되므로 데이터가 없거나 삭제된 경우 공통 예외 클래스로 던집니다.
			.orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

		return ProductDetailResponse.from(product);
	}
}
