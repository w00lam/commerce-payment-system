package com.commercepaymentsystem.domain.product.controller;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.commercepaymentsystem.domain.product.dto.ProductCreateRequest;
import com.commercepaymentsystem.domain.product.dto.ProductCreateResponse;
import com.commercepaymentsystem.domain.product.dto.ProductDeleteResponse;
import com.commercepaymentsystem.domain.product.dto.ProductDetailResponse;
import com.commercepaymentsystem.domain.product.dto.ProductListResponse;
import com.commercepaymentsystem.domain.product.dto.ProductSearchCondition;
import com.commercepaymentsystem.domain.product.dto.ProductUpdateRequest;
import com.commercepaymentsystem.domain.product.dto.ProductUpdateResponse;
import com.commercepaymentsystem.domain.product.service.ProductService;
import com.commercepaymentsystem.global.response.ApiResponse;
import com.commercepaymentsystem.global.response.PageResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

	private final ProductService productService;

	/**
	 * 1. 상품 등록
	 */
	@PostMapping
	public ApiResponse<ProductCreateResponse> createProduct(
		@Valid @RequestBody ProductCreateRequest request
	) {
		ProductCreateResponse response = productService.createProduct(request);
		return ApiResponse.ok(response);
	}

	/**
	 * 2. 상품 목록 조회 (페이징)
	 */
	@GetMapping
	public ApiResponse<PageResponse<ProductListResponse>> getProducts(
		@ModelAttribute ProductSearchCondition condition,
		@PageableDefault(size = 20, sort = "createdAt") Pageable pageable
	) {
		PageResponse<ProductListResponse> response = productService.getProducts(condition, pageable);
		return ApiResponse.ok(response);
	}

	/**
	 * 3. 상품 상세 조회
	 */
	@GetMapping("/{productId}")
	public ApiResponse<ProductDetailResponse> getProductDetail(
		@PathVariable Long productId
	) {
		ProductDetailResponse response = productService.getProductDetail(productId);
		return ApiResponse.ok(response);
	}

	/**
	 * 4. 상품 수정
	 */
	@PutMapping("/{productId}")
	public ApiResponse<ProductUpdateResponse> updateProduct(
		@PathVariable Long productId,
		@Valid @RequestBody ProductUpdateRequest request
	) {
		ProductUpdateResponse response = productService.updateProduct(productId, request);
		return ApiResponse.ok(response);
	}

	/**
	 * 5. 상품 삭제 (Soft Delete)
	 */
	@DeleteMapping("/{productId}")
	public ApiResponse<ProductDeleteResponse> deleteProduct(
		@PathVariable Long productId
	) {
		ProductDeleteResponse response = productService.deleteProduct(productId);
		return ApiResponse.ok(response);
	}
}
