package com.commercepaymentsystem.domain.product.entity;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.commercepaymentsystem.global.exception.BusinessException;

class ProductTest {

	@Test
	@DisplayName("상품을 생성하면 전달한 상태로 생성된다.")
	void createProduct_Success() {
		// given & when
		Product product = Product.create("테스트 상품", 10000L, 100L, "테스트 설명", ProductStatus.ON_SALE, ProductCategory.ELECTRONICS);

		// then
		assertThat(product.getName()).isEqualTo("테스트 상품");
		assertThat(product.getStatus()).isEqualTo(ProductStatus.ON_SALE);
		assertThat(product.getStock()).isEqualTo(100L);
	}

	@Test
	@DisplayName("재고 차감 성공 시 잔여 재고가 감소한다.")
	void removeStock_Success() {
		// given
		Product product = Product.create("테스트 상품", 10000L, 100L, "설명", ProductStatus.ON_SALE, ProductCategory.ELECTRONICS);

		// when
		product.removeStock(30L);

		// then
		assertThat(product.getStock()).isEqualTo(70L);
		assertThat(product.getStatus()).isEqualTo(ProductStatus.ON_SALE);
	}

	@Test
	@DisplayName("재고가 정확히 0이 되도록 차감하면 상태가 SOLD_OUT 으로 변경된다.")
	void removeStock_SoldOut() {
		// given
		Product product = Product.create("테스트 상품", 10000L, 100L, "설명", ProductStatus.ON_SALE, ProductCategory.ELECTRONICS);

		// when
		product.removeStock(100L);

		// then
		assertThat(product.getStock()).isEqualTo(0L);
		assertThat(product.getStatus()).isEqualTo(ProductStatus.SOLD_OUT);
	}

	@Test
	@DisplayName("차감하려는 재고가 현재 재고보다 많으면 예외가 발생한다.")
	void removeStock_Fail_OutOfStock() {
		// given
		Product product = Product.create("테스트 상품", 10000L, 10L, "설명", ProductStatus.ON_SALE, ProductCategory.ELECTRONICS);

		// when & then
		assertThatThrownBy(() -> product.removeStock(20L))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining("재고가 부족합니다");
	}

	@Test
	@DisplayName("차감하려는 재고가 0 이하이거나 null 이면 예외가 발생한다.")
	void removeStock_Fail_InvalidQuantity() {
		// given
		Product product = Product.create("테스트 상품", 10000L, 10L, "설명", ProductStatus.ON_SALE, ProductCategory.ELECTRONICS);

		// when & then
		assertThatThrownBy(() -> product.removeStock(0L))
			.isInstanceOf(BusinessException.class);
			
		assertThatThrownBy(() -> product.removeStock(-1L))
			.isInstanceOf(BusinessException.class);
			
		assertThatThrownBy(() -> product.removeStock(null))
			.isInstanceOf(BusinessException.class);
	}

	@Test
	@DisplayName("품절된 상품에 재고를 추가하면 다시 판매중 상태로 변경된다.")
	void addStock_Success() {
		// given
		Product product = Product.create("테스트 상품", 10000L, 100L, "설명", ProductStatus.ON_SALE, ProductCategory.ELECTRONICS);
		product.removeStock(100L); // 품절 상태 만들기
		assertThat(product.getStatus()).isEqualTo(ProductStatus.SOLD_OUT);

		// when
		product.addStock(50L);

		// then
		assertThat(product.getStock()).isEqualTo(50L);
		assertThat(product.getStatus()).isEqualTo(ProductStatus.ON_SALE);
	}
    
    @Test
	@DisplayName("상품 정보를 재고와 상태를 포함하여 수정할 수 있다.")
	void update_Success() {
		// given
		Product product = Product.create("테스트 상품", 10000L, 100L, "설명", ProductStatus.ON_SALE, ProductCategory.ELECTRONICS);

		// when
		product.update("수정 상품", 20000L, 50L, "수정 설명", ProductStatus.ON_SALE, ProductCategory.FOOD);

		// then
		assertThat(product.getName()).isEqualTo("수정 상품");
		assertThat(product.getPrice()).isEqualTo(20000L);
		assertThat(product.getStock()).isEqualTo(50L);
		assertThat(product.getDescription()).isEqualTo("수정 설명");
		assertThat(product.getStatus()).isEqualTo(ProductStatus.ON_SALE);
		assertThat(product.getCategory()).isEqualTo(ProductCategory.FOOD);
	}
}
