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
	 * 현재 잔액 조회: Member 엔티티에서 pointBalance 조회
	 */
	public PointResponse getMyPoint(Long memberId) {
		Member member = findMemberById(memberId);
		return new PointResponse(member.getPointBalance());
	}

	/**
	 * 거래 내역 조회: Pageable을 이용한 페이징 처리
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
	 * 포인트 적립 (결제 완료 시 등)
	 * 멱등성 보장을 위해 락 획득 후 중복 여부를 재검증함
	 */
	@Transactional
	public void earnPoint(Long memberId, Long amount, Long paymentId) {
		if (amount == null || amount <= 0) {
			throw new PointException(PointErrorCode.INVALID_POINT_AMOUNT);
		}

		if (paymentId == null) {
			throw new PointException(PointErrorCode.PAYMENT_ID_REQUIRED);
		}

		// 1. 비관적 락을 먼저 획득하여 동시성 제어 및 원자적 검증 환경 조성 (Lost Update 방지)
		Member member = memberRepository.findByIdWithPessimisticLock(memberId)
			.orElseThrow(() -> new PointException(MemberErrorCode.MEMBER_NOT_FOUND));

		// 2. 락 획득 상태에서 멱등성 재검증 (중복 적립 방지)
		if (pointHistoryRepository.existsByPaymentIdAndType(paymentId, PointHistoryType.EARN)) {
			throw new PointException(PointErrorCode.ALREADY_EARNED_POINT);
		}

		member.addPoint(amount);

		PointHistory history = new PointHistory(memberId, paymentId, PointHistoryType.EARN, amount);
		pointHistoryRepository.save(history);
	}

	private Member findMemberById(Long memberId) {
		return memberRepository.findById(memberId)
			.orElseThrow(() -> new PointException(MemberErrorCode.MEMBER_NOT_FOUND));
	}

	private void validateMemberExists(Long memberId) {
		if (!memberRepository.existsById(memberId)) {
			throw new PointException(MemberErrorCode.MEMBER_NOT_FOUND);
		}
	}
}
