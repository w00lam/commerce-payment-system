package com.commercepaymentsystem.infrastructure.portone.client;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.commercepaymentsystem.infrastructure.portone.config.PortOneProperties;
import com.commercepaymentsystem.infrastructure.portone.dto.PortOneBillingKeyPaymentRequest;
import com.commercepaymentsystem.infrastructure.portone.dto.PortOneBillingKeyResponse;
import com.commercepaymentsystem.infrastructure.portone.dto.PortOnePaymentCancelRequest;
import com.commercepaymentsystem.infrastructure.portone.dto.PortOnePaymentCancelResponse;
import com.commercepaymentsystem.infrastructure.portone.dto.PortOnePaymentConfirmRequest;
import com.commercepaymentsystem.infrastructure.portone.dto.PortOnePaymentConfirmResponse;
import com.commercepaymentsystem.infrastructure.portone.dto.PortOnePaymentResponse;
import com.commercepaymentsystem.infrastructure.portone.exception.PortOneException;
import com.commercepaymentsystem.infrastructure.portone.exception.PortOneExceptionConverter;
import com.commercepaymentsystem.infrastructure.portone.exception.PortOneRetryableException;

@Component
public class PortOneClient {

	private final RestClient restClient;
	private final PortOneProperties properties;

	public PortOneClient(RestClient portOneRestClient, PortOneProperties properties) {
		this.restClient = portOneRestClient;
		this.properties = properties;
	}

	public PortOnePaymentResponse getPayment(String paymentId) {
		try {
			PortOnePaymentResponse response = restClient.get()
				.uri(uriBuilder -> uriBuilder
					.path("/payments/{paymentId}")
					.queryParam("storeId", properties.storeId())
					.build(paymentId))
				.header(HttpHeaders.AUTHORIZATION, authorizationHeader())
				.retrieve()
				.body(PortOnePaymentResponse.class);

			if (response == null || isBlank(response.id())) {
				throw new PortOneException("PortOne 결제 조회 실패: 결제 응답이 비어 있습니다.");
			}

			return response;
		} catch (RestClientResponseException exception) {
			throw PortOneExceptionConverter.paymentException(exception, "결제 조회");
		} catch (ResourceAccessException exception) {
			throw new PortOneRetryableException("PortOne 재시도 가능 오류: 결제 조회 요청에 실패했습니다.", exception);
		} catch (PortOneException exception) {
			throw exception;
		} catch (RestClientException exception) {
			throw new PortOneException("PortOne 결제 조회 실패: 응답을 해석할 수 없습니다.", exception);
		}
	}

	/**
	 * PortOne V2 빌링키 조회 API를 호출합니다.
	 */
	public PortOneBillingKeyResponse getBillingKey(String billingKey) {
		try {
			PortOneBillingKeyResponse response = restClient.get()
				.uri(uriBuilder -> uriBuilder
					.path("/billing-keys/{billingKey}")
					.queryParam("storeId", properties.storeId())
					.build(billingKey))
				.header(HttpHeaders.AUTHORIZATION, authorizationHeader())
				.retrieve()
				.body(PortOneBillingKeyResponse.class);

			if (response == null || isBlank(response.billingKey())) {
				throw new PortOneException("PortOne 빌링키 조회 실패: 응답이 비어 있습니다.");
			}

			return response;
		} catch (RestClientResponseException exception) {
			throw PortOneExceptionConverter.paymentException(exception, "빌링키 조회");
		} catch (ResourceAccessException exception) {
			throw new PortOneRetryableException("PortOne 재시도 가능 오류: 빌링키 조회 요청에 실패했습니다.", exception);
		} catch (PortOneException exception) {
			throw exception;
		} catch (RestClientException exception) {
			throw new PortOneException("PortOne 빌링키 조회 실패: 응답을 해석할 수 없습니다.", exception);
		}
	}

	/**
	 * PortOne V2 빌링키 삭제 API를 호출합니다.
	 */
	public void deleteBillingKey(String billingKey, String reason) {
		try {
			restClient.delete()
				.uri(uriBuilder -> uriBuilder
					.path("/billing-keys/{billingKey}")
					.queryParam("storeId", properties.storeId())
					.queryParam("reason", reason)
					.build(billingKey))
				.header(HttpHeaders.AUTHORIZATION, authorizationHeader())
				.retrieve()
				.toBodilessEntity();
		} catch (RestClientResponseException exception) {
			throw PortOneExceptionConverter.paymentException(exception, "빌링키 삭제");
		} catch (ResourceAccessException exception) {
			throw new PortOneRetryableException("PortOne 재시도 가능 오류: 빌링키 삭제 요청에 실패했습니다.", exception);
		} catch (PortOneException exception) {
			throw exception;
		} catch (RestClientException exception) {
			throw new PortOneException("PortOne 빌링키 삭제 실패: 응답을 해석할 수 없습니다.", exception);
		}
	}

	/**
	 * PortOne V2 빌링키 결제 API를 호출합니다.
	 */
	public PortOnePaymentResponse payWithBillingKey(String paymentId, PortOneBillingKeyPaymentRequest request) {
		try {
			PayWithBillingKeyResponse response = restClient.post()
				.uri("/payments/{paymentId}/billing-key", paymentId)
				.header(HttpHeaders.AUTHORIZATION, authorizationHeader())
				.body(PortOneBillingKeyPaymentBody.from(properties.storeId(), properties.billingChannelKey(), request))
				.retrieve()
				.body(PayWithBillingKeyResponse.class);

			if (response == null || response.payment() == null || isBlank(response.payment().pgTxId())) {
				throw new PortOneException("PortOne 빌링키 결제 실패: 결제 응답이 비어 있습니다.");
			}

			return new PortOnePaymentResponse(
				paymentId,
				"PAID",
				response.payment().pgTxId(),
				request.orderName(),
				new PortOnePaymentResponse.PortOnePaymentAmount(request.amount().total()),
				response.payment().paidAt(),
				null
			);
		} catch (RestClientResponseException exception) {
			throw PortOneExceptionConverter.paymentException(exception, "빌링키 결제");
		} catch (ResourceAccessException exception) {
			throw new PortOneRetryableException("PortOne 재시도 가능 오류: 빌링키 결제 요청에 실패했습니다.", exception);
		} catch (PortOneException exception) {
			throw exception;
		} catch (RestClientException exception) {
			throw new PortOneException("PortOne 빌링키 결제 실패: 응답을 해석할 수 없습니다.", exception);
		}
	}

	public PortOnePaymentConfirmResponse confirmPayment(String paymentId, PortOnePaymentConfirmRequest request) {
		try {
			PortOnePaymentConfirmResponse response = restClient.post()
				.uri("/payments/{paymentId}/confirm", paymentId)
				.header(HttpHeaders.AUTHORIZATION, authorizationHeader())
				.body(PortOnePaymentConfirmBody.from(properties.storeId(), request))
				.retrieve()
				.body(PortOnePaymentConfirmResponse.class);

			if (response == null || response.transaction() == null) {
				throw new PortOneException("PortOne 결제 승인 실패: 승인 응답이 비어 있습니다.");
			}

			return response;
		} catch (RestClientResponseException exception) {
			throw PortOneExceptionConverter.paymentException(exception, "결제 승인");
		} catch (ResourceAccessException exception) {
			throw new PortOneRetryableException("PortOne 재시도 가능 오류: 결제 승인 요청에 실패했습니다.", exception);
		} catch (PortOneException exception) {
			throw exception;
		} catch (RestClientException exception) {
			throw new PortOneException("PortOne 결제 승인 실패: 응답을 해석할 수 없습니다.", exception);
		}
	}

	/**
	 * PortOne V2 결제 취소 API를 호출합니다.
	 *
	 * 전액 환불과 부분 환불은 같은 취소 API를 사용합니다.
	 * 환불 서비스가 계산한 PG 취소 금액을 amount로 전달하고,
	 * 취소 가능 잔액 검증을 위해 currentCancellableAmount도 함께 전달합니다.
	 */
	public PortOnePaymentCancelResponse cancelPayment(String paymentId, PortOnePaymentCancelRequest request) {
		try {
			PortOnePaymentCancelResponse response = restClient.post()
				.uri("/payments/{paymentId}/cancel", paymentId)
				.header(HttpHeaders.AUTHORIZATION, authorizationHeader())
				.body(PortOnePaymentCancelBody.from(properties.storeId(), request))
				.retrieve()
				.body(PortOnePaymentCancelResponse.class);

			if (response == null || response.cancellation() == null) {
				throw new PortOneException(
					"PortOne 결제 취소 실패: 취소 응답이 비어 있습니다.",
					null,
					null,
					null,
					null
				);
			}

			return response;
		} catch (RestClientResponseException exception) {
			throw PortOneExceptionConverter.cancelException(exception);
		} catch (ResourceAccessException exception) {
			throw new PortOneRetryableException("PortOne 재시도 가능 오류: 결제 취소 요청에 실패했습니다.", exception);
		} catch (PortOneException exception) {
			throw exception;
		} catch (RestClientException exception) {
			throw new PortOneException(
				"PortOne 결제 취소 실패: 응답을 해석할 수 없습니다.",
				null,
				null,
				null,
				exception
			);
		}
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private String authorizationHeader() {
		return "PortOne " + properties.apiSecret();
	}

	private record PortOnePaymentConfirmBody(
		String storeId,
		String paymentToken,
		String txId,
		Long totalAmount
	) {

		private static PortOnePaymentConfirmBody from(String storeId, PortOnePaymentConfirmRequest request) {
			return new PortOnePaymentConfirmBody(
				storeId,
				request.paymentToken(),
				request.txId(),
				request.totalAmount()
			);
		}
	}

	private record PortOnePaymentCancelBody(
		String storeId,
		Long amount,
		Long taxFreeAmount,
		Long currentCancellableAmount,
		String reason,
		String requester
	) {

		private static PortOnePaymentCancelBody from(String storeId, PortOnePaymentCancelRequest request) {
			return new PortOnePaymentCancelBody(
				storeId,
				request.amount(),
				request.taxFreeAmount(),
				request.currentCancellableAmount(),
				request.reason(),
				request.requester()
			);
		}
	}

	private record PortOneBillingKeyPaymentBody(
		String storeId,
		String channelKey,
		String billingKey,
		String orderName,
		PortOneBillingKeyPaymentRequest.PortOneBillingKeyPaymentAmount amount,
		String currency,
		PortOneBillingKeyPaymentRequest.PortOneBillingKeyCustomer customer
	) {
		private static PortOneBillingKeyPaymentBody from(
			String storeId,
			String channelKey,
			PortOneBillingKeyPaymentRequest request
		) {
			return new PortOneBillingKeyPaymentBody(
				storeId,
				channelKey,
				request.billingKey(),
				request.orderName(),
				request.amount(),
				request.currency(),
				request.customer()
			);
		}
	}

	private record PayWithBillingKeyResponse(
		BillingKeyPaymentSummary payment
	) {
	}

	private record BillingKeyPaymentSummary(
		String pgTxId,
		java.time.Instant paidAt
	) {
	}
}
