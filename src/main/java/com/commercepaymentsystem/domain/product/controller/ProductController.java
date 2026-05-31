package com.commercepaymentsystem.domain.product.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.commercepaymentsystem.domain.product.dto.ProductDetailResponse;
import com.commercepaymentsystem.domain.product.dto.ProductListResponse;
import com.commercepaymentsystem.domain.product.dto.ProductSearchCondition;
import com.commercepaymentsystem.domain.product.service.ProductService;
import com.commercepaymentsystem.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;

/**
 * 외부(클라이언트)로부터 상품 관련 요청을 받아 응답을 반환하는 컨트롤러입니다.
 * 컨벤션에 따라 비즈니스 로직은 수행하지 않고 Service로 위임하며, 모든 응답을 ApiResponse로 감싸서 반환합니다.
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

	private final ProductService productService;

	/**
	 * 다중 조건 검색 및 페이징이 적용된 상품 목록을 조회합니다.
	 * URL 예시: /api/products?category=ELECTRONICS&page=0&size=10
	 */
	@GetMapping
	public ApiResponse<Page<ProductListResponse>> getProducts(
		@ModelAttribute ProductSearchCondition condition,
		// 기본적으로 10개씩 최신순(createdAt) 정렬되도록 PageableDefault를 설정했습니다.
		@PageableDefault(size = 10, sort = "createdAt") Pageable pageable
	) {
		Page<ProductListResponse> response = productService.getProducts(condition, pageable);
		return ApiResponse.ok(response); // 성공 응답을 공통 포맷으로 감쌉니다.
	}

	/**
	 * 상품 ID를 통해 특정 상품의 상세 정보를 조회합니다.
	 * URL 예시: /api/products/1
	 */
	@GetMapping("/{productId}")
	public ApiResponse<ProductDetailResponse> getProductDetail(
		@PathVariable Long productId
	) {
		ProductDetailResponse response = productService.getProductDetail(productId);
		return ApiResponse.ok(response);
	}
}
