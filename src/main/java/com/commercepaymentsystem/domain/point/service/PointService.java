package com.commercepaymentsystem.domain.point.service;

import com.commercepaymentsystem.domain.member.entity.Member;
import com.commercepaymentsystem.domain.member.exception.MemberErrorCode;
import com.commercepaymentsystem.domain.member.repository.MemberRepository;
import com.commercepaymentsystem.domain.point.dto.PointHistoryResponse;
import com.commercepaymentsystem.domain.point.dto.PointResponse;
import com.commercepaymentsystem.domain.point.entity.PointHistory;
import com.commercepaymentsystem.domain.point.entity.PointHistoryType;
import com.commercepaymentsystem.domain.point.entity.PointSourceType;
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
	 * 특정 결제 건에 대해 이미 회수 처리된 적립 포인트의 합계를 조회합니다.
	 *
	 * @param paymentId 결제 식별자 (PK)
	 * @return 이미 회수 완료된 적립 포인트 총합
	 */
	public long getRevokedEarnedPointAmount(Long paymentId) {
		return getRevokedEarnedPointAmount(paymentId, PointSourceType.ORDER);
	}

	public long getRevokedEarnedPointAmount(Long paymentId, PointSourceType sourceType) {
		if (paymentId == null) {
			throw pointException(PointErrorCode.PAYMENT_ID_REQUIRED);
		}
		return (sourceType == PointSourceType.ORDER)
			? pointHistoryRepository.sumAmountByPaymentIdAndType(paymentId, PointHistoryType.EARN_REVOKE)
			: pointHistoryRepository.sumAmountByPaymentIdAndTypeAndSourceType(paymentId, PointHistoryType.EARN_REVOKE, sourceType);
	}

	/**
	 * 포인트 적립
	 */
	@Transactional
	public void earnPoint(Long memberId, Long amount, Long paymentId) {
		earnPoint(memberId, amount, paymentId, PointSourceType.ORDER);
	}

	@Transactional
	public void earnPoint(Long memberId, Long amount, Long paymentId, PointSourceType sourceType) {
		validatePointRequest(amount, paymentId);

		Member member = findMemberByIdWithLock(memberId);

		// 멱등성 보장: 이미 적립된 경우 리턴
		boolean exists = (sourceType == PointSourceType.ORDER)
			? pointHistoryRepository.existsByPaymentIdAndType(paymentId, PointHistoryType.EARN)
			: pointHistoryRepository.existsByPaymentIdAndTypeAndSourceType(paymentId, PointHistoryType.EARN, sourceType);

		if (exists) {
			return;
		}

		member.addPoint(amount);
		savePointHistory(memberId, paymentId, null, PointHistoryType.EARN, amount, sourceType);
	}

	/**
	 * 포인트 차감
	 */
	@Transactional
	public void deductPoint(Long memberId, Long amount, Long paymentId) {
		deductPoint(memberId, amount, paymentId, PointSourceType.ORDER);
	}

	@Transactional
	public void deductPoint(Long memberId, Long amount, Long paymentId, PointSourceType sourceType) {
		validatePointRequest(amount, paymentId);

		Member member = findMemberByIdWithLock(memberId);

		// 멱등성 보장: 이미 차감된 경우 리턴
		boolean exists = (sourceType == PointSourceType.ORDER)
			? pointHistoryRepository.existsByPaymentIdAndType(paymentId, PointHistoryType.USE)
			: pointHistoryRepository.existsByPaymentIdAndTypeAndSourceType(paymentId, PointHistoryType.USE, sourceType);

		if (exists) {
			return;
		}

		// 잔액 검증 및 차감 책임은 Member 엔티티에 위임 (중복 로직 제거)
		member.deductPoint(amount);
		savePointHistory(memberId, paymentId, null, PointHistoryType.USE, amount, sourceType);
	}

	/**
	 * 포인트 복구 (결제 취소/부분 환불 시)
	 */
	@Transactional
	public void restorePoint(Long memberId, Long amount, Long paymentId, Long refundId) {
		restorePoint(memberId, amount, paymentId, refundId, PointSourceType.ORDER);
	}

	@Transactional
	public void restorePoint(Long memberId, Long amount, Long paymentId, Long refundId, PointSourceType sourceType) {
		validateRestoreRequest(amount, paymentId, refundId);

		Member member = findMemberByIdWithLock(memberId);

		// 멱등성 보장: 해당 환불 건에 대해 이미 복구된 경우 리턴
		boolean exists = (sourceType == PointSourceType.ORDER)
			? pointHistoryRepository.existsByPaymentIdAndTypeAndRefundId(paymentId, PointHistoryType.USE_CANCEL, refundId)
			: pointHistoryRepository.existsByPaymentIdAndTypeAndRefundIdAndSourceType(paymentId, PointHistoryType.USE_CANCEL, refundId, sourceType);

		if (exists) {
			return;
		}

		// 원본 차감 내역 확인: 사용한 적 없는 포인트를 복구할 수 없음
		boolean hasUsage = (sourceType == PointSourceType.ORDER)
			? pointHistoryRepository.existsByPaymentIdAndType(paymentId, PointHistoryType.USE)
			: pointHistoryRepository.existsByPaymentIdAndTypeAndSourceType(paymentId, PointHistoryType.USE, sourceType);

		if (!hasUsage) {
			throw pointException(PointErrorCode.SOURCE_HISTORY_NOT_FOUND);
		}

		member.addPoint(amount);
		savePointHistory(memberId, paymentId, refundId, PointHistoryType.USE_CANCEL, amount, sourceType);
	}

	/**
	 * 환불 처리 시 결제로 인해 적립된 포인트를 회수(취소)합니다.
	 * 회원의 현재 잔액 내에서 최대한 회수하며, 회수한 금액만큼 포인트 거래 내역을 기록합니다.
	 *
	 * @param memberId 회원 식별자 (PK)
	 * @param amount 회수할 대상 포인트 금액
	 * @param paymentId 결제 식별자 (PK)
	 * @param refundId 환불 식별자 (PK)
	 */
	@Transactional
	public void revokeEarnedPoint(Long memberId, Long amount, Long paymentId, Long refundId) {
		revokeEarnedPoint(memberId, amount, paymentId, refundId, PointSourceType.ORDER);
	}

	@Transactional
	public void revokeEarnedPoint(Long memberId, Long amount, Long paymentId, Long refundId, PointSourceType sourceType) {
		validateRestoreRequest(amount, paymentId, refundId);

		Member member = findMemberByIdWithLock(memberId);

		boolean exists = (sourceType == PointSourceType.ORDER)
			? pointHistoryRepository.existsByPaymentIdAndTypeAndRefundId(paymentId, PointHistoryType.EARN_REVOKE, refundId)
			: pointHistoryRepository.existsByPaymentIdAndTypeAndRefundIdAndSourceType(paymentId, PointHistoryType.EARN_REVOKE, refundId, sourceType);

		if (exists) {
			return;
		}

		boolean hasEarn = (sourceType == PointSourceType.ORDER)
			? pointHistoryRepository.existsByPaymentIdAndType(paymentId, PointHistoryType.EARN)
			: pointHistoryRepository.existsByPaymentIdAndTypeAndSourceType(paymentId, PointHistoryType.EARN, sourceType);

		if (!hasEarn) {
			throw pointException(PointErrorCode.SOURCE_HISTORY_NOT_FOUND);
		}

		Long revokedAmount = member.revokePoint(amount);
		if (revokedAmount <= 0) {
			return;
		}

		savePointHistory(memberId, paymentId, refundId, PointHistoryType.EARN_REVOKE, revokedAmount, sourceType);
	}

	private void validatePointRequest(Long amount, Long paymentId) {
		if (amount == null || amount <= 0) {
			throw pointException(PointErrorCode.INVALID_POINT_AMOUNT);
		}
		if (paymentId == null) {
			throw pointException(PointErrorCode.PAYMENT_ID_REQUIRED);
		}
	}

	private void validateRestoreRequest(Long amount, Long paymentId, Long refundId) {
		validatePointRequest(amount, paymentId);
		if (refundId == null) {
			throw pointException(PointErrorCode.REFUND_ID_REQUIRED);
		}
	}

	private Member findMemberByIdWithLock(Long memberId) {
		return memberRepository.findByIdWithPessimisticLock(memberId)
			.orElseThrow(this::memberNotFound);
	}

	private void savePointHistory(Long memberId, Long paymentId, PointHistoryType type, Long amount) {
		savePointHistory(memberId, paymentId, null, type, amount, PointSourceType.ORDER);
	}

	private void savePointHistory(Long memberId, Long paymentId, Long refundId, PointHistoryType type, Long amount, PointSourceType sourceType) {
		PointHistory history = new PointHistory(memberId, paymentId, refundId, type, amount, sourceType);
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
