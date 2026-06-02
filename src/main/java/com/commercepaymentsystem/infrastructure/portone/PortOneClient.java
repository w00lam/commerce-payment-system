package com.commercepaymentsystem.infrastructure.portone;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.commercepaymentsystem.infrastructure.portone.config.PortOneProperties;
import com.commercepaymentsystem.infrastructure.portone.dto.PortOnePaymentCancelRequest;
import com.commercepaymentsystem.infrastructure.portone.dto.PortOnePaymentCancelResponse;
import com.commercepaymentsystem.infrastructure.portone.dto.PortOnePaymentConfirmRequest;
import com.commercepaymentsystem.infrastructure.portone.dto.PortOnePaymentConfirmResponse;
import com.commercepaymentsystem.infrastructure.portone.dto.PortOnePaymentResponse;
import com.commercepaymentsystem.infrastructure.portone.exception.PortOneException;
import com.commercepaymentsystem.infrastructure.portone.exception.PortOnePaymentVerificationException;
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
				.uri("/payments/{paymentId}", paymentId)
				.header(HttpHeaders.AUTHORIZATION, authorizationHeader())
				.retrieve()
				.body(PortOnePaymentResponse.class);

			if (response == null || isBlank(response.id())) {
				throw new PortOnePaymentVerificationException(
					"PortOne 결제 조회 실패: 결제 응답이 비어 있습니다."
				);
			}

			return response;
		} catch (RestClientResponseException exception) {
			throw convertPaymentException(exception);
		} catch (ResourceAccessException exception) {
			throw new PortOneRetryableException("PortOne 재시도 가능 오류: 결제 조회 요청에 실패했습니다.", exception);
		} catch (PortOnePaymentVerificationException | PortOneRetryableException exception) {
			throw exception;
		} catch (RestClientException exception) {
			throw new PortOnePaymentVerificationException(
				"PortOne 결제 조회 실패: 응답을 해석할 수 없습니다.",
				exception
			);
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
				throw new PortOnePaymentVerificationException(
					"PortOne 결제 승인 실패: 승인 응답이 비어 있습니다."
				);
			}

			return response;
		} catch (RestClientResponseException exception) {
			throw convertPaymentException(exception, "결제 승인");
		} catch (ResourceAccessException exception) {
			throw new PortOneRetryableException("PortOne 재시도 가능 오류: 결제 승인 요청에 실패했습니다.", exception);
		} catch (PortOnePaymentVerificationException | PortOneRetryableException exception) {
			throw exception;
		} catch (RestClientException exception) {
			throw new PortOnePaymentVerificationException(
				"PortOne 결제 승인 실패: 응답을 해석할 수 없습니다.",
				exception
			);
		}
	}

	public PortOnePaymentCancelResponse cancelPayment(String paymentId, PortOnePaymentCancelRequest request) {
		try {
			PortOnePaymentCancelResponse response = restClient.post()
				.uri("/payments/{paymentId}/cancel", paymentId)
				.header(HttpHeaders.AUTHORIZATION, authorizationHeader())
				.body(PortOnePaymentCancelBody.from(properties.storeId(), request))
				.retrieve()
				.body(PortOnePaymentCancelResponse.class);

			if (response == null || response.cancellation() == null) {
				throw new PortOnePaymentVerificationException(
					"PortOne 결제 취소 실패: 취소 응답이 비어 있습니다."
				);
			}

			return response;
		} catch (RestClientResponseException exception) {
			throw convertPaymentException(exception, "결제 취소");
		} catch (ResourceAccessException exception) {
			throw new PortOneRetryableException("PortOne 재시도 가능 오류: 결제 취소 요청에 실패했습니다.", exception);
		} catch (PortOnePaymentVerificationException | PortOneRetryableException exception) {
			throw exception;
		} catch (RestClientException exception) {
			throw new PortOnePaymentVerificationException(
				"PortOne 결제 취소 실패: 응답을 해석할 수 없습니다.",
				exception
			);
		}
	}

	private PortOneException convertPaymentException(RestClientResponseException exception) {
		return convertPaymentException(exception, "결제 조회");
	}

	private PortOneException convertPaymentException(RestClientResponseException exception, String operationName) {
		HttpStatusCode statusCode = exception.getStatusCode();

		if (statusCode.is5xxServerError()) {
			return new PortOneRetryableException("PortOne 재시도 가능 오류: " + operationName + " 요청에 실패했습니다.", exception);
		}

		return new PortOnePaymentVerificationException("PortOne " + operationName + " 실패", exception);
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
		String reason,
		String requester
	) {

		private static PortOnePaymentCancelBody from(String storeId, PortOnePaymentCancelRequest request) {
			return new PortOnePaymentCancelBody(
				storeId,
				request.amount(),
				request.taxFreeAmount(),
				request.reason(),
				request.requester()
			);
		}
	}
}
