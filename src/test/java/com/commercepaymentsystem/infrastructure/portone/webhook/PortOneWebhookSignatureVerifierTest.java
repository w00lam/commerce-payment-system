package com.commercepaymentsystem.infrastructure.portone.webhook;

import static org.assertj.core.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.commercepaymentsystem.domain.webhook.exception.WebhookException;

class PortOneWebhookSignatureVerifierTest {

	private static final String SECRET = System.getenv("PORTONE_WEBHOOK_SECRET");

	private final PortOneWebhookSignatureVerifier verifier = new PortOneWebhookSignatureVerifier(
		new PortOneWebhookProperties(SECRET)
	);

	@Test
	@DisplayName("Standard Webhooks signature verification succeeds")
	void verify_success() {
		String payload = paidPayload();
		String timestamp = String.valueOf(Instant.now().getEpochSecond());
		String signature = "v1," + sign("msg-123." + timestamp + "." + payload);

		assertThatCode(() -> verifier.verify("msg-123", timestamp, signature, payload))
			.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("Invalid webhook signature is rejected")
	void verify_invalidSignature_fail() {
		String payload = paidPayload();
		String timestamp = String.valueOf(Instant.now().getEpochSecond());

		assertThatThrownBy(() -> verifier.verify("msg-123", timestamp, "v1,invalid", payload))
			.isInstanceOf(WebhookException.class);
	}

	@Test
	@DisplayName("Old webhook timestamp is rejected to prevent replay")
	void verify_oldTimestamp_fail() {
		String payload = paidPayload();
		String oldTimestamp = String.valueOf(Instant.now().minusSeconds(600).getEpochSecond());
		String signature = "v1," + sign("msg-123." + oldTimestamp + "." + payload);

		assertThatThrownBy(() -> verifier.verify("msg-123", oldTimestamp, signature, payload))
			.isInstanceOf(WebhookException.class);
	}

	private String paidPayload() {
		return """
			{"type":"Transaction.Paid","timestamp":"2026-06-04T00:00:00Z","data":{"paymentId":"payment-123","storeId":"store-123","transactionId":"tx-123"}}
			""".trim();
	}

	private String sign(String message) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			String value = SECRET.startsWith("whsec_") ? SECRET.substring(6) : SECRET;
			mac.init(new SecretKeySpec(Base64.getDecoder().decode(value), "HmacSHA256"));
			return Base64.getEncoder().encodeToString(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception exception) {
			throw new IllegalStateException(exception);
		}
	}
}
