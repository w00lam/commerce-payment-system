package com.commercepaymentsystem.infrastructure.portone;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.http.HttpMethod.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.test.web.client.ExpectedCount.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.commercepaymentsystem.infrastructure.portone.exception.PortOneAuthenticationException;
import com.commercepaymentsystem.infrastructure.portone.exception.PortOnePaymentVerificationException;
import com.commercepaymentsystem.infrastructure.portone.exception.PortOneRetryableException;

class PortOneClientTest {

	private PortOneClient portOneClient;
	private MockRestServiceServer server;

	@BeforeEach
	void setUp() {
		RestClient.Builder restClientBuilder = RestClient.builder()
			.baseUrl("https://api.portone.test");
		server = MockRestServiceServer.bindTo(restClientBuilder).build();

		PortOneProperties properties = new PortOneProperties(
			"https://api.portone.test",
			"test-api-secret",
			Duration.ofSeconds(1),
			Duration.ofSeconds(3)
		);
		portOneClient = new PortOneClient(restClientBuilder.build(), properties);
	}

	@Test
	@DisplayName("API Secret으로 access token을 발급한다")
	void issueAccessToken_success() {
		// given
		server.expect(once(), requestTo("https://api.portone.test/login/api-secret"))
			.andExpect(method(POST))
			.andExpect(header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
			.andExpect(content().json("""
				{
				  "apiSecret": "test-api-secret"
				}
				"""))
			.andRespond(withSuccess("""
				{
				  "accessToken": "access-token",
				  "refreshToken": "refresh-token"
				}
				""", APPLICATION_JSON));

		// when
		String accessToken = portOneClient.issueAccessToken();

		// then
		assertThat(accessToken).isEqualTo("access-token");
		server.verify();
	}

	@Test
	@DisplayName("토큰 발급 인증 실패 응답은 인증 예외로 변환한다")
	void issueAccessToken_unauthorized_fail() {
		// given
		server.expect(once(), requestTo("https://api.portone.test/login/api-secret"))
			.andRespond(withUnauthorizedRequest());

		// when & then
		assertThatThrownBy(() -> portOneClient.issueAccessToken())
			.isInstanceOf(PortOneAuthenticationException.class)
			.hasMessageContaining("PortOne 인증 실패");
		server.verify();
	}

	@Test
	@DisplayName("PortOne 외부 결제 단건 조회는 Bearer 토큰을 전송하고 결제 정보를 매핑한다")
	void getPayment_success() {
		// given
		server.expect(once(), requestTo("https://api.portone.test/login/api-secret"))
			.andRespond(withSuccess("""
				{
				  "accessToken": "access-token",
				  "refreshToken": "refresh-token"
				}
				""", APPLICATION_JSON));
		server.expect(once(), requestTo("https://api.portone.test/payments/payment-123"))
			.andExpect(method(GET))
			.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
			.andRespond(withSuccess("""
				{
				  "id": "payment-123",
				  "status": "PAID",
				  "transactionId": "tx-123",
				  "orderName": "order-10",
				  "amount": {
				    "total": 10000
				  },
				  "paidAt": "2026-06-01T01:02:03Z"
				}
				""", APPLICATION_JSON));

		// when
		PortOnePaymentResponse payment = portOneClient.getPayment("payment-123");

		// then
		assertThat(payment.id()).isEqualTo("payment-123");
		assertThat(payment.status()).isEqualTo("PAID");
		assertThat(payment.transactionId()).isEqualTo("tx-123");
		assertThat(payment.orderName()).isEqualTo("order-10");
		assertThat(payment.totalAmount()).isEqualTo(10_000L);
		assertThat(payment.paidAt()).isEqualTo(Instant.parse("2026-06-01T01:02:03Z"));
		server.verify();
	}

	@Test
	@DisplayName("PortOne 외부 결제 단건 조회 404 응답은 결제 조회 실패 예외로 변환한다")
	void getPayment_notFound_fail() {
		// given
		server.expect(once(), requestTo("https://api.portone.test/login/api-secret"))
			.andRespond(withSuccess("""
				{
				  "accessToken": "access-token",
				  "refreshToken": "refresh-token"
				}
				""", APPLICATION_JSON));
		server.expect(once(), requestTo("https://api.portone.test/payments/missing-payment"))
			.andRespond(withResourceNotFound());

		// when & then
		assertThatThrownBy(() -> portOneClient.getPayment("missing-payment"))
			.isInstanceOf(PortOnePaymentVerificationException.class)
			.hasMessageContaining("PortOne 결제 조회 실패");
		server.verify();
	}

	@Test
	@DisplayName("PortOne 외부 결제 단건 조회 5xx 응답은 재시도 가능 예외로 변환한다")
	void getPayment_serverError_fail() {
		// given
		server.expect(once(), requestTo("https://api.portone.test/login/api-secret"))
			.andRespond(withSuccess("""
				{
				  "accessToken": "access-token",
				  "refreshToken": "refresh-token"
				}
				""", APPLICATION_JSON));
		server.expect(once(), requestTo("https://api.portone.test/payments/payment-123"))
			.andRespond(withServerError());

		// when & then
		assertThatThrownBy(() -> portOneClient.getPayment("payment-123"))
			.isInstanceOf(PortOneRetryableException.class)
			.hasMessageContaining("PortOne 재시도 가능 오류");
		server.verify();
	}
}
