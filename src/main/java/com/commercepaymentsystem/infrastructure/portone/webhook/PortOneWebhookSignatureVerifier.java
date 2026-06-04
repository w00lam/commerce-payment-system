package com.commercepaymentsystem.infrastructure.portone.webhook;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

import com.commercepaymentsystem.domain.webhook.exception.WebhookErrorCode;
import com.commercepaymentsystem.domain.webhook.exception.WebhookException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PortOneWebhookSignatureVerifier {

	private static final String SECRET_PREFIX = "whsec_";
	private static final String SIGNATURE_PREFIX = "v1,";
	private static final String HMAC_SHA256 = "HmacSHA256";

	private final PortOneWebhookProperties webhookProperties;

	public void verify(
		String eventId,
		String timestamp,
		String signatureHeader,
		String payload
	) {
		if (isBlank(eventId) || isBlank(timestamp) || isBlank(signatureHeader) || payload == null) {
			throw new WebhookException(WebhookErrorCode.INVALID_SIGNATURE);
		}

		String secret = webhookProperties.secret();
		if (isBlank(secret)) {
			throw new WebhookException(WebhookErrorCode.INVALID_SIGNATURE, "웹훅 시크릿이 설정되지 않았습니다.");
		}

		String expectedSignature = sign(eventId + "." + timestamp + "." + payload, secret);
		if (!containsValidSignature(signatureHeader, expectedSignature)) {
			throw new WebhookException(WebhookErrorCode.INVALID_SIGNATURE);
		}
	}

	private String sign(String message, String secret) {
		try {
			Mac mac = Mac.getInstance(HMAC_SHA256);
			mac.init(new SecretKeySpec(decodeSecret(secret), HMAC_SHA256));
			byte[] signature = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(signature);
		} catch (Exception exception) {
			throw new WebhookException(WebhookErrorCode.INVALID_SIGNATURE);
		}
	}

	private byte[] decodeSecret(String secret) {
		String value = secret.startsWith(SECRET_PREFIX)
			? secret.substring(SECRET_PREFIX.length())
			: secret;
		return Base64.getDecoder().decode(value);
	}

	private boolean containsValidSignature(String signatureHeader, String expectedSignature) {
		for (String token : signatureHeader.split(" ")) {
			String signature = token.trim();
			if (signature.startsWith(SIGNATURE_PREFIX)) {
				signature = signature.substring(SIGNATURE_PREFIX.length());
			}
			if (MessageDigest.isEqual(
				signature.getBytes(StandardCharsets.UTF_8),
				expectedSignature.getBytes(StandardCharsets.UTF_8)
			)) {
				return true;
			}
		}
		return false;
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
