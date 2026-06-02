package com.commercepaymentsystem.domain.product.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commercepaymentsystem.domain.product.dto.ProductCreateRequest;
import com.commercepaymentsystem.domain.product.dto.ProductCreateResponse;
import com.commercepaymentsystem.domain.product.dto.ProductDeleteResponse;
import com.commercepaymentsystem.domain.product.dto.ProductDetailResponse;
import com.commercepaymentsystem.domain.product.dto.ProductListResponse;
import com.commercepaymentsystem.domain.product.dto.ProductSearchCondition;
import com.commercepaymentsystem.domain.product.dto.ProductUpdateRequest;
import com.commercepaymentsystem.domain.product.dto.ProductUpdateResponse;
import com.commercepaymentsystem.domain.product.entity.Product;
import com.commercepaymentsystem.domain.product.exception.ProductErrorCode;
import com.commercepaymentsystem.domain.product.repository.ProductRepository;
import com.commercepaymentsystem.domain.product.repository.ProductSpecification;
import com.commercepaymentsystem.global.exception.BusinessException;
import com.commercepaymentsystem.global.response.PageResponse;

import lombok.RequiredArgsConstructor;

/**
 * 상품(Product) 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 상품 등록, 조회(페이징 및 검색), 수정, 삭제 기능을 제공합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

	private final ProductRepository productRepository;

	/**
	 * 1. 상품 등록
	 */
	/**
	 * 새로운 상품을 등록합니다.
	 *
	 * @param request 상품 등록 요청 데이터 (이름, 가격, 재고 등)
	 * @return 등록된 상품 정보 (ProductCreateResponse)
	 */
	@Transactional
	public ProductCreateResponse createProduct(ProductCreateRequest request) {
		Product product = Product.create(
			request.name(),
			request.price(),
			request.stock(),
			request.description(),
			request.status(),
			request.category()
		);
		Product savedProduct = productRepository.save(product);
		return ProductCreateResponse.from(savedProduct);
	}

	/**
	 * 2. 상품 목록 조회 (페이징)
	 */
	/**
	 * 동적 검색 조건과 페이징을 적용하여 상품 목록을 조회합니다.
	 *
	 * @param condition 검색 조건 (상품명, 카테고리, 상태 등)
	 * @param pageable  페이징 정보
	 * @return 상품 목록 페이지 응답 (PageResponse<ProductListResponse>)
	 */
	public PageResponse<ProductListResponse> getProducts(ProductSearchCondition condition, Pageable pageable) {
		Page<Product> productPage = productRepository.findAll(
			ProductSpecification.searchWith(condition),
			pageable
		);
		return PageResponse.from(productPage.map(ProductListResponse::from));
	}

	/**
	 * 3. 상품 상세 조회
	 */
	/**
	 * 특정 상품의 상세 정보를 조회합니다.
	 *
	 * @param productId 조회할 상품 ID
	 * @return 상품 상세 정보 (ProductDetailResponse)
	 * @throws BusinessException 상품이 존재하지 않을 경우
	 */
	public ProductDetailResponse getProductDetail(Long productId) {
		Product product = findProductById(productId);
		return ProductDetailResponse.from(product);
	}

	/**
	 * 4. 상품 수정
	 */
	/**
	 * 특정 상품의 정보를 수정합니다.
	 *
	 * @param productId 수정할 상품 ID
	 * @param request   수정할 데이터
	 * @return 수정된 상품 정보 (ProductUpdateResponse)
	 */
	@Transactional
	public ProductUpdateResponse updateProduct(Long productId, ProductUpdateRequest request) {
		Product product = findProductById(productId);
		
		product.update(
			request.name(),
			request.price(),
			request.stock(),
			request.description(),
			request.status(),
			request.category()
		);
		
		return ProductUpdateResponse.from(product);
	}

	/**
	 * 5. 상품 삭제 (Soft Delete)
	 */
	/**
	 * 특정 상품을 논리적 삭제(Soft Delete) 처리합니다.
	 * DB에서 완전히 삭제하지 않고 삭제 일시(deletedAt)를 기록합니다.
	 *
	 * @param productId 삭제할 상품 ID
	 * @return 삭제 완료 메시지 (ProductDeleteResponse)
	 */
	@Transactional
	public ProductDeleteResponse deleteProduct(Long productId) {
		Product product = findProductById(productId);
		product.delete();
		// JPA 환경에서는 @Transactional 종료 시 더티 체킹으로 자동 UPDATE 처리됨
		return ProductDeleteResponse.of(productId);
	}

	/**
	 * 내부 공통 메서드: ID로 상품 조회
	 */
	private Product findProductById(Long productId) {
		return productRepository.findById(productId)
			.orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));
	}
}
