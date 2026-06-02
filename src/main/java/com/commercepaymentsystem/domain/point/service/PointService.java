package com.commercepaymentsystem.domain.point.service;

import com.commercepaymentsystem.domain.member.entity.Member;
import com.commercepaymentsystem.domain.member.exception.MemberErrorCode;
import com.commercepaymentsystem.domain.member.repository.MemberRepository;
import com.commercepaymentsystem.domain.point.dto.PointHistoryResponse;
import com.commercepaymentsystem.domain.point.dto.PointResponse;
import com.commercepaymentsystem.domain.point.entity.PointHistory;
import com.commercepaymentsystem.domain.point.entity.PointHistoryType;
import com.commercepaymentsystem.domain.point.exception.PointErrorCode;
import com.commercepaymentsystem.domain.point.exception.PointException;
import com.commercepaymentsystem.domain.point.repository.PointHistoryRepository;
import com.commercepaymentsystem.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointService {

	private final MemberRepository memberRepository;
	private final PointHistoryRepository pointHistoryRepository;

	/**
	 * 현재 잔액 조회
	 */
	public PointResponse getMyPoint(Long memberId) {
		Member member = findMemberById(memberId);
		return new PointResponse(member.getPointBalance());
	}

	/**
	 * 거래 내역 조회
	 */
	public Page<PointHistoryResponse> getMyPointHistories(Long memberId, Pageable pageable) {
		validateMemberExists(memberId);
		return pointHistoryRepository.findByMemberId(memberId, pageable)
			.map(history -> new PointHistoryResponse(
				history.getType().name(),
				history.getAmount(),
				history.getCreatedAt()
			));
	}

	/**
	 * 포인트 적립
	 */
	@Transactional
	public void earnPoint(Long memberId, Long amount, Long paymentId) {
		validatePointRequest(amount, paymentId);

		Member member = findMemberByIdWithLock(memberId);

		// 멱등성 보장: 이미 적립된 경우 리턴 (리뷰 반영)
		if (pointHistoryRepository.existsByPaymentIdAndType(paymentId, PointHistoryType.EARN)) {
			return;
		}

		member.addPoint(amount);
		savePointHistory(memberId, paymentId, PointHistoryType.EARN, amount);
	}

	/**
	 * 포인트 차감
	 */
	@Transactional
	public void deductPoint(Long memberId, Long amount, Long paymentId) {
		validatePointRequest(amount, paymentId);

		Member member = findMemberByIdWithLock(memberId);

		// 멱등성 보장: 이미 차감된 경우 리턴
		if (pointHistoryRepository.existsByPaymentIdAndType(paymentId, PointHistoryType.USE)) {
			return;
		}

		member.deductPoint(amount);
		savePointHistory(memberId, paymentId, PointHistoryType.USE, amount);
	}

	private void validatePointRequest(Long amount, Long paymentId) {
		if (amount == null || amount <= 0) {
			throw new PointException(PointErrorCode.INVALID_POINT_AMOUNT);
		}
		if (paymentId == null) {
			throw new PointException(PointErrorCode.PAYMENT_ID_REQUIRED);
		}
	}

	private Member findMemberByIdWithLock(Long memberId) {
		return memberRepository.findByIdWithPessimisticLock(memberId)
			.orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
	}

	private void savePointHistory(Long memberId, Long paymentId, PointHistoryType type, Long amount) {
		PointHistory history = new PointHistory(memberId, paymentId, type, amount);
		pointHistoryRepository.save(history);
	}

	private Member findMemberById(Long memberId) {
		return memberRepository.findById(memberId)
			.orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
	}

	private void validateMemberExists(Long memberId) {
		if (!memberRepository.existsById(memberId)) {
			throw new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND);
		}
	}
}
