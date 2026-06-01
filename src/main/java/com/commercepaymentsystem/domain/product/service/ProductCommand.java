package com.commercepaymentsystem.domain.product.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.commercepaymentsystem.domain.product.entity.Product;
import com.commercepaymentsystem.domain.product.exception.ProductErrorCode;
import com.commercepaymentsystem.domain.product.repository.ProductRepository;
import com.commercepaymentsystem.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * 다른 도메인(장바구니 등)에서 Product 도메인에 접근하기 위한 파사드(Facade) 컴포넌트입니다.
 * 외부 도메인이 ProductRepository에 직접 의존하는 것을 방지하고, 상품 조회 및 검증 책임을 캡슐화합니다.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductCommand {

    private final ProductRepository productRepository;

    /**
     * 특정 상품 단건을 조회합니다. 상품이 없으면 예외를 발생시킵니다.
     */
    public Product getProductForCart(Long productId) {
        return productRepository.findById(productId)
            .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));
    }

    /**
     * 여러 상품의 정보를 한 번에 조회하여 Map 형태로 반환합니다.
     */
    public Map<Long, Product> getProductsForCart(List<Long> productIds) {
        return productRepository.findAllById(productIds).stream()
            .collect(Collectors.toMap(Product::getId, p -> p));
    }
}
