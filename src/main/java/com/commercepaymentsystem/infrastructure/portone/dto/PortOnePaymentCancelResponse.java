package com.commercepaymentsystem.infrastructure.portone.dto;

import java.time.Instant;

/**
 * PortOne 결제 취소 API 응답입니다.
 *
 * @param cancellation PortOne이 반환한 취소 처리 상세 정보
 */
public record PortOnePaymentCancelResponse(
	PortOnePaymentCancellation cancellation
) {

	/**
	 * 결제 취소 처리 상세 정보입니다.
	 *
	 * @param id PortOne 취소 식별자
	 * @param status 취소 처리 상태
	 * @param pgCancellationId PG사 취소 식별자
	 * @param totalAmount 취소된 총 금액
	 * @param pgCode PG사 응답 코드
	 * @param pgMessage PG사 응답 메시지
	 * @param reason 취소 사유
	 * @param requestedAt 취소 요청 시각
	 * @param cancelledAt 취소 완료 시각
	 */
	public record PortOnePaymentCancellation(
		String id,
		String status,
		String pgCancellationId,
		Long totalAmount,
		String pgCode,
		String pgMessage,
		String reason,
		Instant requestedAt,
		Instant cancelledAt
	) {
	}
}
