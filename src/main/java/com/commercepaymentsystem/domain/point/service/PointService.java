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

		// 멱등성 보장: 이미 적립된 경우 리턴
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

		// 잔액 검증 및 차감 책임은 Member 엔티티에 위임 (중복 로직 제거)
		member.deductPoint(amount);
		savePointHistory(memberId, paymentId, PointHistoryType.USE, amount);
	}

	/**
	 * 포인트 복구 (결제 취소/부분 환불 시)
	 */
	@Transactional
	public void restorePoint(Long memberId, Long amount, Long paymentId, Long refundId) {
		validatePointRequest(amount, paymentId);

		Member member = findMemberByIdWithLock(memberId);

		// 멱등성 보장: 해당 환불 건에 대해 이미 복구된 경우 리턴
		if (pointHistoryRepository.existsByPaymentIdAndTypeAndRefundId(paymentId, PointHistoryType.USE_CANCEL, refundId)) {
			return;
		}

		// 원본 차감 내역 확인: 사용한 적 없는 포인트를 복구할 수 없음
		if (!pointHistoryRepository.existsByPaymentIdAndType(paymentId, PointHistoryType.USE)) {
			throw pointException(PointErrorCode.SOURCE_HISTORY_NOT_FOUND);
		}

		member.addPoint(amount);
		savePointHistory(memberId, paymentId, refundId, PointHistoryType.USE_CANCEL, amount);
	}

	private void validatePointRequest(Long amount, Long paymentId) {
		if (amount == null || amount <= 0) {
			throw pointException(PointErrorCode.INVALID_POINT_AMOUNT);
		}
		if (paymentId == null) {
			throw pointException(PointErrorCode.PAYMENT_ID_REQUIRED);
		}
	}

	private Member findMemberByIdWithLock(Long memberId) {
		return memberRepository.findByIdWithPessimisticLock(memberId)
			.orElseThrow(this::memberNotFound);
	}

	private void savePointHistory(Long memberId, Long paymentId, PointHistoryType type, Long amount) {
		savePointHistory(memberId, paymentId, null, type, amount);
	}

	private void savePointHistory(Long memberId, Long paymentId, Long refundId, PointHistoryType type, Long amount) {
		PointHistory history = new PointHistory(memberId, paymentId, refundId, type, amount);
		pointHistoryRepository.save(history);
	}

	private Member findMemberById(Long memberId) {
		return memberRepository.findById(memberId)
			.orElseThrow(this::memberNotFound);
	}

	private void validateMemberExists(Long memberId) {
		if (!memberRepository.existsById(memberId)) {
			throw memberNotFound();
		}
	}

	private BusinessException memberNotFound() {
		return new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND);
	}

	private PointException pointException(PointErrorCode errorCode) {
		return new PointException(errorCode);
	}
}
