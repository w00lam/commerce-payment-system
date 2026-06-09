package com.commercepaymentsystem.infrastructure.portone.client;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.http.HttpMethod.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.test.web.client.ExpectedCount.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.client.MockRestServiceServer;
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
			"test-billing-channel-key",
			Duration.ofSeconds(1),
			Duration.ofSeconds(3)
		);
		portOneClient = new PortOneClient(restClientBuilder.build(), properties);
	}

	@Test
	@DisplayName("결제 조회 시 API Secret 인증 헤더와 storeId를 전송한다")
	void getPayment_success() {
		server.expect(once(), requestTo("https://api.portone.test/payments/payment-123?storeId=test-store-id"))
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

		PortOnePaymentResponse payment = portOneClient.getPayment("payment-123");

		assertThat(payment.id()).isEqualTo("payment-123");
		assertThat(payment.status()).isEqualTo("PAID");
		assertThat(payment.transactionId()).isEqualTo("tx-123");
		assertThat(payment.orderName()).isEqualTo("order-10");
		assertThat(payment.totalAmount()).isEqualTo(10_000L);
		assertThat(payment.paidAt()).isEqualTo(Instant.parse("2026-06-01T01:02:03Z"));
		server.verify();
	}

	@Test
	@DisplayName("결제 승인 시 API Secret 인증 헤더로 승인 API를 호출한다")
	void confirmPayment_success() {
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

		PortOnePaymentConfirmResponse response = portOneClient.confirmPayment(
			"payment-123",
			new PortOnePaymentConfirmRequest("payment-token", "tx-123", 10_000L)
		);

		assertThat(response.transaction().pgTxId()).isEqualTo("pg-tx-123");
		assertThat(response.transaction().paidAt()).isEqualTo(Instant.parse("2026-06-01T01:02:03Z"));
		server.verify();
	}

	@Test
	@DisplayName("결제 취소 시 API Secret 인증 헤더로 취소 API를 호출한다")
	void cancelPayment_success() {
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

		PortOnePaymentCancelResponse response = portOneClient.cancelPayment(
			"payment-123",
			new PortOnePaymentCancelRequest(5_000L, 0L, 10_000L, "customer-request", "CUSTOMER")
		);

		assertThat(response.cancellation().id()).isEqualTo("cancel-123");
		assertThat(response.cancellation().status()).isEqualTo("SUCCEEDED");
		assertThat(response.cancellation().totalAmount()).isEqualTo(5_000L);
		assertThat(response.cancellation().pgCode()).isEqualTo("00");
		assertThat(response.cancellation().pgMessage()).isEqualTo("cancel approved");
		server.verify();
	}

	@Test
	@DisplayName("결제 취소 401 응답은 재시도하지 않는 예외로 변환된다")
	void cancelPayment_unauthorized_fail() {
		server.expect(once(), requestTo("https://api.portone.test/payments/payment-123/cancel"))
			.andRespond(withUnauthorizedRequest());

		assertThatThrownBy(() -> portOneClient.cancelPayment(
			"payment-123",
			new PortOnePaymentCancelRequest(5_000L, 0L, 10_000L, "customer-request", "CUSTOMER")
		))
			.isInstanceOf(PortOneException.class)
			.hasMessageContaining("PortOne 인증 실패");
		server.verify();
	}

	@Test
	@DisplayName("결제 취소 PG 거절 응답은 PG 필드를 포함한 예외로 변환된다")
	void cancelPayment_pgRejection_fail() {
		server.expect(once(), requestTo("https://api.portone.test/payments/payment-123/cancel"))
			.andRespond(withBadRequest().body("""
				{
				  "type": "PgProviderError",
				  "message": "PG rejected cancel request",
				  "pgCode": "DUPLICATED_CANCEL",
				  "pgMessage": "already cancelled"
				}
				""").contentType(APPLICATION_JSON));

		assertThatThrownBy(() -> portOneClient.cancelPayment(
			"payment-123",
			new PortOnePaymentCancelRequest(5_000L, 0L, 10_000L, "customer-request", "CUSTOMER")
		))
			.isInstanceOf(PortOneException.class)
			.hasMessageContaining("PortOne 결제 취소 실패")
			.hasMessageContaining("PG rejected cancel request")
			.extracting("statusCode", "errorType", "portOneMessage", "pgCode", "pgMessage", "responseBody")
			.containsExactly(
				400,
				"PgProviderError",
				"PG rejected cancel request",
				"DUPLICATED_CANCEL",
				"already cancelled",
				"""
				{
				  "type": "PgProviderError",
				  "message": "PG rejected cancel request",
				  "pgCode": "DUPLICATED_CANCEL",
				  "pgMessage": "already cancelled"
				}
				"""
		);
		server.verify();
	}

	@Test
	@DisplayName("결제 취소 실패 응답의 이스케이프 문자열을 JSON으로 파싱한다")
	void cancelPayment_pgRejectionWithEscapedMessage_fail() {
		server.expect(once(), requestTo("https://api.portone.test/payments/payment-123/cancel"))
			.andRespond(withBadRequest().body("""
				{
				  "type": "PgProviderError",
				  "message": "PG rejected \\"cancel\\" request",
				  "pgCode": "DUPLICATED_CANCEL",
				  "pgMessage": "already cancelled"
				}
				""").contentType(APPLICATION_JSON));

		assertThatThrownBy(() -> portOneClient.cancelPayment(
			"payment-123",
			new PortOnePaymentCancelRequest(5_000L, 0L, 10_000L, "customer-request", "CUSTOMER")
		))
			.isInstanceOf(PortOneException.class)
			.extracting("portOneMessage")
			.isEqualTo("PG rejected \"cancel\" request");
		server.verify();
	}

	@Test
	@DisplayName("결제 취소 네트워크 오류는 재시도 가능 예외로 변환된다")
	void cancelPayment_network_fail() {
		server.expect(once(), requestTo("https://api.portone.test/payments/payment-123/cancel"))
			.andRespond(withException(new IOException("timeout")));

		assertThatThrownBy(() -> portOneClient.cancelPayment(
			"payment-123",
			new PortOnePaymentCancelRequest(5_000L, 0L, 10_000L, "customer-request", "CUSTOMER")
		))
			.isInstanceOf(PortOneRetryableException.class)
			.hasMessageContaining("PortOne 재시도 가능 오류");
		server.verify();
	}

	@Test
	@DisplayName("결제 조회 404 응답은 재시도하지 않는 예외로 변환된다")
	void getPayment_notFound_fail() {
		server.expect(once(), requestTo("https://api.portone.test/payments/missing-payment?storeId=test-store-id"))
			.andRespond(withResourceNotFound());

		assertThatThrownBy(() -> portOneClient.getPayment("missing-payment"))
			.isInstanceOf(PortOneException.class)
			.hasMessageContaining("PortOne 결제 조회 실패");
		server.verify();
	}

	@Test
	@DisplayName("결제 조회 5xx 응답은 재시도 가능 예외로 변환된다")
	void getPayment_serverError_fail() {
		server.expect(once(), requestTo("https://api.portone.test/payments/payment-123?storeId=test-store-id"))
			.andRespond(withServerError());

		assertThatThrownBy(() -> portOneClient.getPayment("payment-123"))
			.isInstanceOf(PortOneRetryableException.class)
			.hasMessageContaining("PortOne 재시도 가능 오류");
		server.verify();
	}

	@Test
	@DisplayName("빌링키 조회 성공 시 빌링키 상세 정보를 반환한다")
	void getBillingKey_success() {
		server.expect(once(), requestTo("https://api.portone.test/billing-keys/billing-key-123?storeId=test-store-id"))
			.andExpect(method(GET))
			.andExpect(header(HttpHeaders.AUTHORIZATION, "PortOne test-api-secret"))
			.andRespond(withSuccess("""
				{
				  "billingKey": "billing-key-123",
				  "status": "ISSUED",
				  "issuedAt": "2026-06-01T01:02:03Z",
				  "method": {
				    "type": "CARD",
				    "card": {
				      "name": "TossCard",
				      "number": "1234-****-****-5678",
				      "bin": "123456"
				    }
				  },
				  "customer": {
				    "id": "customer-123"
				  }
				}
				""", APPLICATION_JSON));

		var response = portOneClient.getBillingKey("billing-key-123");

		assertThat(response.billingKey()).isEqualTo("billing-key-123");
		assertThat(response.status()).isEqualTo("ISSUED");
		assertThat(response.method().card().name()).isEqualTo("TossCard");
		server.verify();
	}

	@Test
	@DisplayName("빌링키 삭제 호출 시 DELETE 요청을 전송한다")
	void deleteBillingKey_success() {
		server.expect(once(), requestTo("https://api.portone.test/billing-keys/billing-key-123?storeId=test-store-id&reason=user-request"))
			.andExpect(method(DELETE))
			.andExpect(header(HttpHeaders.AUTHORIZATION, "PortOne test-api-secret"))
			.andRespond(withNoContent());

		assertThatCode(() -> portOneClient.deleteBillingKey("billing-key-123", "user-request"))
			.doesNotThrowAnyException();
		server.verify();
	}

	@Test
	@DisplayName("빌링키 결제 요청 시 POST 요청을 전송하고 결제 응답을 반환한다")
	void payWithBillingKey_success() {
		server.expect(once(), requestTo("https://api.portone.test/payments/payment-123/billing-key"))
			.andExpect(method(POST))
			.andExpect(header(HttpHeaders.AUTHORIZATION, "PortOne test-api-secret"))
			.andExpect(content().json("""
				{
				  "storeId": "test-store-id",
				  "channelKey": "test-billing-channel-key",
				  "billingKey": "billing-key-123",
				  "orderName": "Monthly Subscription",
				  "amount": { "total": 10000 },
				  "currency": "KRW"
				}
				"""))
			.andRespond(withSuccess("""
				{
				  "payment": {
				    "pgTxId": "pg-tx-123",
				    "paidAt": "2026-06-01T01:02:03Z"
				  }
				}
				""", APPLICATION_JSON));

		var request = new com.commercepaymentsystem.infrastructure.portone.dto.PortOneBillingKeyPaymentRequest(
			"billing-key-123",
			"Monthly Subscription",
			new com.commercepaymentsystem.infrastructure.portone.dto.PortOneBillingKeyPaymentRequest.PortOneBillingKeyPaymentAmount(10_000L),
			"KRW",
			new com.commercepaymentsystem.infrastructure.portone.dto.PortOneBillingKeyPaymentRequest.PortOneBillingKeyCustomer("customer-123")
		);

		PortOnePaymentResponse response = portOneClient.payWithBillingKey("payment-123", request);

		assertThat(response.id()).isEqualTo("payment-123");
		assertThat(response.status()).isEqualTo("PAID");
		assertThat(response.transactionId()).isEqualTo("pg-tx-123");
		assertThat(response.paidAt()).isEqualTo(Instant.parse("2026-06-01T01:02:03Z"));
		server.verify();
	}
}
