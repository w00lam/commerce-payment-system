package com.commercepaymentsystem.infrastructure.portone;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.commercepaymentsystem.infrastructure.portone.exception.PortOneAuthenticationException;
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

	public String issueAccessToken() {
		try {
			// PortOne V2 인증이 필요한 결제 API 호출 전에 API Secret으로 access token을 발급받는다.
			PortOneAccessTokenResponse response = restClient.post()
				.uri("/login/api-secret")
				.body(new PortOneAccessTokenRequest(properties.apiSecret()))
				.retrieve()
				.body(PortOneAccessTokenResponse.class);

			if (response == null || isBlank(response.accessToken())) {
				throw new PortOneAuthenticationException("PortOne 인증 실패: access token 응답이 비어 있습니다.");
			}

			return response.accessToken();
		} catch (RestClientResponseException exception) {
			throw convertAuthenticationException(exception);
		} catch (ResourceAccessException exception) {
			throw new PortOneRetryableException("PortOne 재시도 가능 오류: 인증 요청에 실패했습니다.", exception);
		} catch (PortOneAuthenticationException | PortOneRetryableException exception) {
			throw exception;
		} catch (RestClientException exception) {
			throw new PortOneAuthenticationException("PortOne 인증 실패: 응답을 해석할 수 없습니다.", exception);
		}
	}

	public PortOnePaymentResponse getPayment(String paymentId) {
		String accessToken = issueAccessToken();

		try {
			// 이후 결제 검증에 사용할 PortOne 외부 결제 단건 조회 응답을 가져온다.
			PortOnePaymentResponse response = restClient.get()
				.uri("/payments/{paymentId}", paymentId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
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

	private PortOneException convertAuthenticationException(RestClientResponseException exception) {
		HttpStatusCode statusCode = exception.getStatusCode();

		// PortOne 서버 오류는 재시도 가능하지만, 그 외 인증 실패는 설정 확인이 필요하다.
		if (statusCode.is5xxServerError()) {
			return new PortOneRetryableException("PortOne 재시도 가능 오류: 인증 요청에 실패했습니다.", exception);
		}

		return new PortOneAuthenticationException("PortOne 인증 실패", exception);
	}

	private PortOneException convertPaymentException(RestClientResponseException exception) {
		HttpStatusCode statusCode = exception.getStatusCode();

		// 일시적인 외부 장애와 확정적인 결제 검증 실패를 구분한다.
		if (statusCode.is5xxServerError()) {
			return new PortOneRetryableException("PortOne 재시도 가능 오류: 결제 조회 요청에 실패했습니다.", exception);
		}

		return new PortOnePaymentVerificationException("PortOne 결제 조회 실패", exception);
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private record PortOneAccessTokenRequest(
		String apiSecret
	) {
	}
}
