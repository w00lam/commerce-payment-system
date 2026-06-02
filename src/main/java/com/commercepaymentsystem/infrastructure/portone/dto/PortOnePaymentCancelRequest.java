package com.commercepaymentsystem.infrastructure.portone.dto;

/**
 * PortOne 결제 취소 API에 전달할 취소 요청 값입니다.
 *
 * @param amount 이번 요청에서 취소할 PG 금액
 * @param taxFreeAmount 이번 취소 금액 중 면세 금액
 * @param currentCancellableAmount 취소 전 PortOne 결제 잔여 금액 검증값
 * @param reason 취소 사유
 * @param requester 취소 요청 주체
 */
public record PortOnePaymentCancelRequest(
	Long amount,
	Long taxFreeAmount,
	Long currentCancellableAmount,
	String reason,
	String requester
) {
}
