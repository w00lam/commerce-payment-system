package com.commercepaymentsystem.infrastructure.portone.client;

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
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import com.commercepaymentsystem.infrastructure.portone.config.PortOneProperties;
import com.commercepaymentsystem.infrastructure.portone.dto.PortOnePaymentCancelRequest;
import com.commercepaymentsystem.infrastructure.portone.dto.PortOnePaymentCancelResponse;
import com.commercepaymentsystem.infrastructure.portone.dto.PortOnePaymentConfirmRequest;
import com.commercepaymentsystem.infrastructure.portone.dto.PortOnePaymentConfirmResponse;
import com.commercepaymentsystem.infrastructure.portone.dto.PortOnePaymentResponse;
import com.commercepaymentsystem.infrastructure.portone.exception.PortOneException;
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
			"test-store-id",
			"test-channel-key",
			Duration.ofSeconds(1),
			Duration.ofSeconds(3)
		);
		portOneClient = new PortOneClient(restClientBuilder.build(), properties);
	}

	@Test
	@DisplayName("PortOne V2 payment lookup sends API Secret authorization and maps payment response")
	void getPayment_success() {
		// given
		server.expect(once(), requestTo("https://api.portone.test/payments/payment-123"))
			.andExpect(method(GET))
			.andExpect(header(HttpHeaders.AUTHORIZATION, "PortOne test-api-secret"))
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
	@DisplayName("PortOne V2 payment confirm calls /payments/{paymentId}/confirm with API Secret authorization")
	void confirmPayment_success() {
		// given
		server.expect(once(), requestTo("https://api.portone.test/payments/payment-123/confirm"))
			.andExpect(method(POST))
			.andExpect(header(HttpHeaders.AUTHORIZATION, "PortOne test-api-secret"))
			.andExpect(content().json("""
				{
				  "storeId": "test-store-id",
				  "paymentToken": "payment-token",
				  "txId": "tx-123",
				  "totalAmount": 10000
				}
				"""))
			.andRespond(withSuccess("""
				{
				  "transaction": {
				    "pgTxId": "pg-tx-123",
				    "paidAt": "2026-06-01T01:02:03Z"
				  }
				}
				""", APPLICATION_JSON));

		// when
		PortOnePaymentConfirmResponse response = portOneClient.confirmPayment(
			"payment-123",
			new PortOnePaymentConfirmRequest("payment-token", "tx-123", 10_000L)
		);

		// then
		assertThat(response.transaction().pgTxId()).isEqualTo("pg-tx-123");
		assertThat(response.transaction().paidAt()).isEqualTo(Instant.parse("2026-06-01T01:02:03Z"));
		server.verify();
	}

	@Test
	@DisplayName("PortOne V2 payment cancel calls /payments/{paymentId}/cancel with API Secret authorization")
	void cancelPayment_success() {
		// given
		server.expect(once(), requestTo("https://api.portone.test/payments/payment-123/cancel"))
			.andExpect(method(POST))
			.andExpect(header(HttpHeaders.AUTHORIZATION, "PortOne test-api-secret"))
			.andExpect(content().json("""
				{
				  "storeId": "test-store-id",
				  "amount": 5000,
				  "taxFreeAmount": 0,
				  "currentCancellableAmount": 10000,
				  "reason": "customer-request",
				  "requester": "CUSTOMER"
				}
				"""))
			.andRespond(withSuccess("""
				{
				  "cancellation": {
				    "id": "cancel-123",
				    "status": "SUCCEEDED",
				    "pgCancellationId": "pg-cancel-123",
				    "totalAmount": 5000,
				    "pgCode": "00",
				    "pgMessage": "cancel approved",
				    "reason": "customer-request",
				    "requestedAt": "2026-06-01T01:02:03Z",
				    "cancelledAt": "2026-06-01T01:03:03Z"
				  }
				}
				""", APPLICATION_JSON));

		// when
		PortOnePaymentCancelResponse response = portOneClient.cancelPayment(
			"payment-123",
			new PortOnePaymentCancelRequest(5_000L, 0L, 10_000L, "customer-request", "CUSTOMER")
		);

		// then
		assertThat(response.cancellation().id()).isEqualTo("cancel-123");
		assertThat(response.cancellation().status()).isEqualTo("SUCCEEDED");
		assertThat(response.cancellation().totalAmount()).isEqualTo(5_000L);
		assertThat(response.cancellation().pgCode()).isEqualTo("00");
		assertThat(response.cancellation().pgMessage()).isEqualTo("cancel approved");
		server.verify();
	}

	@Test
	@DisplayName("PortOne V2 payment cancel 401 response becomes non-retryable exception")
	void cancelPayment_unauthorized_fail() {
		// given
		server.expect(once(), requestTo("https://api.portone.test/payments/payment-123/cancel"))
			.andRespond(withUnauthorizedRequest());

		// when & then
		assertThatThrownBy(() -> portOneClient.cancelPayment(
			"payment-123",
			new PortOnePaymentCancelRequest(5_000L, 0L, 10_000L, "customer-request", "CUSTOMER")
		))
			.isInstanceOf(PortOneException.class)
			.hasMessageContaining("PortOne 인증 실패");
		server.verify();
	}

	@Test
	@DisplayName("PortOne V2 payment cancel PG rejection becomes non-retryable exception with PG fields")
	void cancelPayment_pgRejection_fail() {
		// given
		server.expect(once(), requestTo("https://api.portone.test/payments/payment-123/cancel"))
			.andRespond(withBadRequest().body("""
				{
				  "type": "PgProviderError",
				  "message": "PG rejected cancel request",
				  "pgCode": "DUPLICATED_CANCEL",
				  "pgMessage": "already cancelled"
				}
				""").contentType(APPLICATION_JSON));

		// when & then
		assertThatThrownBy(() -> portOneClient.cancelPayment(
			"payment-123",
			new PortOnePaymentCancelRequest(5_000L, 0L, 10_000L, "customer-request", "CUSTOMER")
		))
			.isInstanceOf(PortOneException.class)
			.hasMessageContaining("PortOne 결제 취소 실패")
			.extracting("errorType", "pgCode", "pgMessage")
			.containsExactly("PgProviderError", "DUPLICATED_CANCEL", "already cancelled");
		server.verify();
	}

	@Test
	@DisplayName("PortOne V2 payment cancel network failure becomes retryable exception")
	void cancelPayment_network_fail() {
		// given
		server.expect(once(), requestTo("https://api.portone.test/payments/payment-123/cancel"))
			.andRespond(withException(new ResourceAccessException("timeout")));

		// when & then
		assertThatThrownBy(() -> portOneClient.cancelPayment(
			"payment-123",
			new PortOnePaymentCancelRequest(5_000L, 0L, 10_000L, "customer-request", "CUSTOMER")
		))
			.isInstanceOf(PortOneRetryableException.class)
			.hasMessageContaining("PortOne 재시도 가능 오류");
		server.verify();
	}

	@Test
	@DisplayName("PortOne V2 payment lookup 404 response becomes non-retryable exception")
	void getPayment_notFound_fail() {
		// given
		server.expect(once(), requestTo("https://api.portone.test/payments/missing-payment"))
			.andRespond(withResourceNotFound());

		// when & then
		assertThatThrownBy(() -> portOneClient.getPayment("missing-payment"))
			.isInstanceOf(PortOneException.class)
			.hasMessageContaining("PortOne 결제 조회 실패");
		server.verify();
	}

	@Test
	@DisplayName("PortOne V2 payment lookup 5xx response becomes retryable exception")
	void getPayment_serverError_fail() {
		// given
		server.expect(once(), requestTo("https://api.portone.test/payments/payment-123"))
			.andRespond(withServerError());

		// when & then
		assertThatThrownBy(() -> portOneClient.getPayment("payment-123"))
			.isInstanceOf(PortOneRetryableException.class)
			.hasMessageContaining("PortOne 재시도 가능 오류");
		server.verify();
	}
}
