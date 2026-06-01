package com.commercepaymentsystem.domain.product.entity;

import java.time.LocalDateTime;

import com.commercepaymentsystem.domain.product.exception.ProductErrorCode;
import com.commercepaymentsystem.global.entity.BaseEntity;
import com.commercepaymentsystem.global.exception.BusinessException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 상품(Product) 도메인의 핵심 데이터베이스 매핑 클래스입니다.
 * ERD 명세에 따라 필요한 컬럼들을 정의하고 있습니다.
 */
@SQLRestriction("deleted_at IS NULL")
@Entity
@Getter
@Table(name = "products")
// 기본 생성자는 JPA 프록시 객체 생성을 위해 필요하지만, 외부에서 빈 객체를 막기 위해 PROTECTED로 설정합니다.
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, columnDefinition = "INT UNSIGNED")
    private Long price; // INT_UNSIGNED에 맞춰 Long 사용

    @Column(nullable = false, columnDefinition = "INT UNSIGNED")
    private Long stock; // 재고 수량

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProductStatus status; // Enum 값을 문자열로 DB에 저장하기 위해 EnumType.STRING 사용

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ProductCategory category;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt; // 소프트 딜리트(Soft Delete)를 위한 삭제 일시

    /**
     * 객체 생성을 담당하는 정적 팩토리 메서드입니다.
     * 무분별한 new 키워드 사용을 막고, 객체가 유효한 상태로만 생성되도록 강제합니다.
     */
    public static Product create(
            String name,
            Long price,
            Long stock,
            String description,
            ProductStatus status,
            ProductCategory category) {
        Product product = new Product(
                null,
                name,
                price,
                stock,
                description,
                status,
                category,
                null);
        return product;
    }

    /**
     * 상품 재고를 차감합니다.
     * 비즈니스 규칙에 따라 재고가 부족할 경우 예외를 발생시킵니다.
     *
     * @param quantity 차감할 재고 수량
     */
    public void removeStock(Long quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BusinessException(ProductErrorCode.INVALID_QUANTITY);
        }
        if (this.stock < quantity) {
            throw new BusinessException(ProductErrorCode.OUT_OF_STOCK);
        }
        this.stock -= quantity;
        if (this.stock == 0) {
            this.status = ProductStatus.SOLD_OUT;
        }
    }

    /**
     * 상품 재고를 추가합니다. (예: 주문 취소, 입고 등)
     * 품절 상태였고 재고가 양수가 되면 다시 판매중 상태로 변경합니다.
     *
     * @param quantity 추가할 재고 수량
     */
    public void addStock(Long quantity) {
        if (quantity != null && quantity > 0) {
            this.stock += quantity;
            if (this.status == ProductStatus.SOLD_OUT && this.stock > 0) {
                this.status = ProductStatus.ON_SALE;
            }
        }
    }

    /**
     * 상품 정보를 수정합니다. (명세서 기준 재고 및 상태 덮어쓰기 포함)
     */
    public void update(String name, Long price, Long stock, String description, ProductStatus status, ProductCategory category) {
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.description = description;
        this.category = category;

        // 도메인 규칙에 따라 재고와 상태 불일치 보정
        if (this.stock == 0) {
            this.status = ProductStatus.SOLD_OUT;
        } else if (this.stock > 0 && status == ProductStatus.SOLD_OUT) {
            this.status = ProductStatus.ON_SALE;
        } else {
            this.status = status;
        }
    }

    /**
     * 상품을 소프트 삭제(Soft Delete) 처리합니다.
     */
    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }
}
