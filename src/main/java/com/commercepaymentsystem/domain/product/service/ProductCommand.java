package com.commercepaymentsystem.domain.product.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
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

    /**
     * 주문 또는 주문서 미리보기에 사용할 상품 목록을 Map 형태로 조회합니다.
     *
     * <p>주문 흐름에서는 장바구니 상품에 연결된 상품이 모두 존재해야 하므로,
     * 요청한 상품 중 하나라도 존재하지 않으면 예외를 발생시킵니다.</p>
     *
     * @param productIds 중복 제거가 완료된 상품 ID 목록
     * @return 상품 ID를 key로 갖는 상품 Map
     */
    public Map<Long, Product> getProductsForOrder(List<Long> productIds) {
        List<Product> products = productRepository.findAllById(productIds);

        if (products.size() != productIds.size()) {
            throw new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND);
        }

        return products.stream()
            .collect(Collectors.toMap(
                Product::getId,
                Function.identity()
            ));
    }
}
