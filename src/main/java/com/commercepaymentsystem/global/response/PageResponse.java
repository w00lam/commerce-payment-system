package com.commercepaymentsystem.global.response;

import java.util.List;

import org.springframework.data.domain.Page;

/**
 * Spring Data의 Page 객체 반환 시 불필요한 필드들을 숨기고
 * API 명세서에 정의된 totalElements, totalPages, content 구조만을 노출하기 위한 래퍼 클래스입니다.
 */
public record PageResponse<T>(
    long totalElements,
    int totalPages,
    List<T> content
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
            page.getTotalElements(),
            page.getTotalPages(),
            page.getContent()
        );
    }
}
