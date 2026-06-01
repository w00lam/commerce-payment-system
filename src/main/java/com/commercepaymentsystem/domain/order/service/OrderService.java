package com.commercepaymentsystem.domain.order.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commercepaymentsystem.domain.cart.entity.CartItem;
import com.commercepaymentsystem.domain.cart.repository.CartItemRepository;
import com.commercepaymentsystem.domain.member.entity.Member;
import com.commercepaymentsystem.domain.member.exception.MemberErrorCode;
import com.commercepaymentsystem.domain.member.repository.MemberRepository;
import com.commercepaymentsystem.domain.order.dto.OrderPreviewItemResponse;
import com.commercepaymentsystem.domain.order.dto.OrderPreviewRequest;
import com.commercepaymentsystem.domain.order.dto.OrderPreviewResponse;
import com.commercepaymentsystem.domain.order.exception.OrderErrorCode;
import com.commercepaymentsystem.domain.order.mapper.OrderPreviewMapper;
import com.commercepaymentsystem.domain.product.exception.ProductErrorCode;
import com.commercepaymentsystem.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

	private final MemberRepository memberRepository;
	private final CartItemRepository cartItemRepository;

	public OrderPreviewResponse previewOrder(
		Long memberId,
		OrderPreviewRequest request
	) {
		Member member = findMember(memberId);

		List<CartItem> cartItems = findPreviewCartItems(
			memberId,
			request.cartItemIds()
		);

		validateCartItems(cartItems);
		validateProductStock(cartItems);

		Long totalAmount = calculateTotalAmount(cartItems);

		List<OrderPreviewItemResponse> items = cartItems.stream()
			.map(OrderPreviewMapper::toItemResponse)
			.toList();

		return OrderPreviewMapper.toResponse(
			member.getId(),
			totalAmount,
			items
		);
	}

	private Member findMember(Long memberId) {
		return memberRepository.findById(memberId)
			.orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
	}

	private List<CartItem> findPreviewCartItems(
		Long memberId,
		List<Long> cartItemIds
	) {
		if (cartItemIds == null || cartItemIds.isEmpty()) {
			return cartItemRepository.findAllByMemberId(memberId);
		}

		List<CartItem> cartItems = cartItemRepository.findAllByMemberIdAndIdIn(
			memberId,
			cartItemIds
		);

		if (cartItems.size() != cartItemIds.size()) {
			throw new BusinessException(OrderErrorCode.INVALID_CART_ITEM);
		}

		return cartItems;
	}

	private void validateCartItems(List<CartItem> cartItems) {
		if (cartItems.isEmpty()) {
			throw new BusinessException(OrderErrorCode.EMPTY_CART);
		}
	}

	private void validateProductStock(List<CartItem> cartItems) {
		for (CartItem cartItem : cartItems) {
			if (cartItem.getProduct().getStock() < cartItem.getQuantity()) {
				throw new BusinessException(ProductErrorCode.OUT_OF_STOCK);
			}
		}
	}

	private Long calculateTotalAmount(List<CartItem> cartItems) {
		return cartItems.stream()
			.mapToLong(cartItem -> cartItem.getProduct().getPrice() * cartItem.getQuantity())
			.sum();
	}
}