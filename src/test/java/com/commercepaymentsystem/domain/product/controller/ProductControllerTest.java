package com.commercepaymentsystem.domain.product.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.commercepaymentsystem.domain.product.dto.ProductCreateRequest;
import com.commercepaymentsystem.domain.product.dto.ProductCreateResponse;
import com.commercepaymentsystem.domain.product.entity.ProductCategory;
import com.commercepaymentsystem.domain.product.entity.ProductStatus;
import com.commercepaymentsystem.domain.product.service.ProductService;
import com.commercepaymentsystem.global.response.ApiResponse;

/**
 * Spring Context (MockMvc, Jackson) 로딩 실패 문제를 방지하기 위해 순수 Mockito 기반으로 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

	@InjectMocks
	private ProductController productController;

	@Mock
	private ProductService productService;

	@Test
	@DisplayName("상품 등록 API 호출 시 성공 응답을 반환한다.")
	void createProduct_Success() {
		// given
		ProductCreateRequest request = new ProductCreateRequest("테스트 키보드", 45000L, 100L, "설명", ProductStatus.ON_SALE, ProductCategory.ELECTRONICS);
		ProductCreateResponse response = new ProductCreateResponse(1L, "테스트 키보드", 45000L, 100L, "설명", ProductStatus.ON_SALE, ProductCategory.ELECTRONICS, LocalDateTime.now());
		
		given(productService.createProduct(any(ProductCreateRequest.class))).willReturn(response);

		// when
		ApiResponse<ProductCreateResponse> apiResponse = productController.createProduct(request);

		// then
		assertThat(apiResponse.getCode()).isEqualTo("SUCCESS");
		assertThat(apiResponse.getMessage()).isEqualTo("요청 성공");
		assertThat(apiResponse.getData().id()).isEqualTo(1L);
		assertThat(apiResponse.getData().name()).isEqualTo("테스트 키보드");
	}
}
