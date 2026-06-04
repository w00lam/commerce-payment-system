package com.commercepaymentsystem.infrastructure.portone.webhook;

import static org.assertj.core.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.commercepaymentsystem.domain.webhook.exception.WebhookException;

class PortOneWebhookSignatureVerifierTest {

	private static final String SECRET = "whsec_d2ViaG9va190ZXN0X3NlY3JldA==";

	private final PortOneWebhookSignatureVerifier verifier = new PortOneWebhookSignatureVerifier(
		new PortOneWebhookProperties(SECRET)
	);

	@Test
	@DisplayName("Standard Webhooks signature verification succeeds")
	void verify_success() {
		String payload = "{\"type\":\"Transaction.Paid\",\"data\":{\"paymentId\":\"payment-123\"}}";
		String signature = "v1," + sign("msg-123.1717460000." + payload);

		assertThatCode(() -> verifier.verify("msg-123", "1717460000", signature, payload))
			.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("Invalid webhook signature is rejected")
	void verify_invalidSignature_fail() {
		String payload = "{\"type\":\"Transaction.Paid\",\"data\":{\"paymentId\":\"payment-123\"}}";

		assertThatThrownBy(() -> verifier.verify("msg-123", "1717460000", "v1,invalid", payload))
			.isInstanceOf(WebhookException.class);
	}

	private String sign(String message) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(Base64.getDecoder().decode("d2ViaG9va190ZXN0X3NlY3JldA=="), "HmacSHA256"));
			return Base64.getEncoder().encodeToString(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception exception) {
			throw new IllegalStateException(exception);
		}
	}
}
