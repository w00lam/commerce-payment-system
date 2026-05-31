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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

	private final ProductRepository productRepository;

	/**
	 * 1. 상품 등록
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
	public ProductDetailResponse getProductDetail(Long productId) {
		Product product = findProductById(productId);
		return ProductDetailResponse.from(product);
	}

	/**
	 * 4. 상품 수정
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
