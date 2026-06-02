package com.commercepaymentsystem.domain.cart.service;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.commercepaymentsystem.domain.cart.entity.CartItem;
import com.commercepaymentsystem.domain.cart.repository.CartItemRepository;

import lombok.RequiredArgsConstructor;

/**
 * 다른 도메인에서 CartItem 도메인에 접근하기 위한 파사드 컴포넌트입니다.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartItemCommand {

	private final CartItemRepository cartItemRepository;

	/**
	 * 회원 ID를 기준으로 전체 장바구니 상품 목록을 조회합니다.
	 *
	 * @param memberId 회원 ID
	 * @return 장바구니 상품 목록
	 */
	public List<CartItem> getCartItemsByMemberId(Long memberId) {
		return cartItemRepository.findAllByMemberId(memberId);
	}

	/**
	 * 회원 ID와 장바구니 상품 ID 목록을 기준으로 장바구니 상품 목록을 조회합니다.
	 *
	 * @param memberId 회원 ID
	 * @param cartItemIds 장바구니 상품 ID 목록
	 * @return 장바구니 상품 목록
	 */
	public List<CartItem> getCartItemsByMemberIdAndIds(
		Long memberId,
		List<Long> cartItemIds
	) {
		return cartItemRepository.findAllByMemberIdAndIdIn(
			memberId,
			cartItemIds
		);
	}
}