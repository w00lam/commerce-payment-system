package com.commercepaymentsystem.domain.product.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.commercepaymentsystem.domain.product.dto.ProductCreateRequest;
import com.commercepaymentsystem.domain.product.dto.ProductCreateResponse;
import com.commercepaymentsystem.domain.product.dto.ProductDeleteResponse;
import com.commercepaymentsystem.domain.product.dto.ProductDetailResponse;
import com.commercepaymentsystem.domain.product.dto.ProductListResponse;
import com.commercepaymentsystem.domain.product.dto.ProductSearchCondition;
import com.commercepaymentsystem.domain.product.dto.ProductUpdateRequest;
import com.commercepaymentsystem.domain.product.dto.ProductUpdateResponse;
import com.commercepaymentsystem.domain.product.entity.Product;
import com.commercepaymentsystem.domain.product.entity.ProductCategory;
import com.commercepaymentsystem.domain.product.entity.ProductStatus;
import com.commercepaymentsystem.domain.product.repository.ProductRepository;
import com.commercepaymentsystem.global.exception.BusinessException;
import com.commercepaymentsystem.global.response.PageResponse;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

	@InjectMocks
	private ProductService productService;

	@Mock
	private ProductRepository productRepository;

	@Test
	@DisplayName("상품 등록을 성공적으로 수행한다.")
	void createProduct_Success() {
		// given
		ProductCreateRequest request = new ProductCreateRequest("새 상품", 15000L, 50L, "설명", ProductStatus.ON_SALE, ProductCategory.ELECTRONICS);
		Product product = Product.create("새 상품", 15000L, 50L, "설명", ProductStatus.ON_SALE, ProductCategory.ELECTRONICS);
		given(productRepository.save(ArgumentMatchers.any(Product.class))).willReturn(product);

		// when
		ProductCreateResponse response = productService.createProduct(request);

		// then
		assertThat(response.name()).isEqualTo("새 상품");
		assertThat(response.stock()).isEqualTo(50L);
	}

	@Test
	@DisplayName("상품 목록을 조건에 맞게 조회한다.")
	void getProducts_Success() {
		// given
		Product product = Product.create("아이폰", 1000L, 10L, "설명", ProductStatus.ON_SALE, ProductCategory.ELECTRONICS);
		Page<Product> productPage = new PageImpl<>(List.of(product));
		
		given(productRepository.findAll(ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Product>>any(), ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)))
			.willReturn(productPage);

		// when
		ProductSearchCondition condition = new ProductSearchCondition(ProductCategory.ELECTRONICS, null);
		PageResponse<ProductListResponse> response = productService.getProducts(condition, PageRequest.of(0, 10));

		// then
		assertThat(response.totalElements()).isEqualTo(1);
		assertThat(response.content()).hasSize(1);
		assertThat(response.content().get(0).name()).isEqualTo("아이폰");
	}

	@Test
	@DisplayName("상품 상세 정보를 성공적으로 조회한다.")
	void getProductDetail_Success() {
		// given
		Product product = Product.create("아이폰", 1000L, 10L, "설명", ProductStatus.ON_SALE, ProductCategory.ELECTRONICS);
		given(productRepository.findById(1L)).willReturn(Optional.of(product));

		// when
		ProductDetailResponse response = productService.getProductDetail(1L);

		// then
		assertThat(response.name()).isEqualTo("아이폰");
		assertThat(response.stock()).isEqualTo(10L);
	}

	@Test
	@DisplayName("존재하지 않는 상품 조회 시 예외가 발생한다.")
	void getProductDetail_Fail_NotFound() {
		// given
		given(productRepository.findById(1L)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> productService.getProductDetail(1L))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining("상품을 찾을 수 없습니다");
	}

	@Test
	@DisplayName("상품 수정을 성공적으로 수행한다.")
	void updateProduct_Success() {
		// given
		Product product = Product.create("기존 상품", 1000L, 10L, "기존 설명", ProductStatus.ON_SALE, ProductCategory.ELECTRONICS);
		given(productRepository.findById(1L)).willReturn(Optional.of(product));
		ProductUpdateRequest request = new ProductUpdateRequest("수정 상품", 2000L, 20L, "수정 설명", ProductStatus.SOLD_OUT, ProductCategory.FOOD);

		// when
		ProductUpdateResponse response = productService.updateProduct(1L, request);

		// then
		assertThat(response.name()).isEqualTo("수정 상품");
		assertThat(response.price()).isEqualTo(2000L);
		assertThat(product.getStock()).isEqualTo(20L); // 객체 내부 상태 변경 확인
		assertThat(product.getStatus()).isEqualTo(ProductStatus.SOLD_OUT);
	}

	@Test
	@DisplayName("상품 삭제(Soft Delete)를 성공적으로 수행한다.")
	void deleteProduct_Success() {
		// given
		Product product = Product.create("삭제할 상품", 1000L, 10L, "설명", ProductStatus.ON_SALE, ProductCategory.ELECTRONICS);
		given(productRepository.findById(1L)).willReturn(Optional.of(product));

		// when
		ProductDeleteResponse response = productService.deleteProduct(1L);

		// then
		assertThat(response.id()).isEqualTo(1L);
		assertThat(response.message()).isEqualTo("정상적으로 삭제 처리되었습니다.");
		assertThat(product.getDeletedAt()).isNotNull();
	}
}
