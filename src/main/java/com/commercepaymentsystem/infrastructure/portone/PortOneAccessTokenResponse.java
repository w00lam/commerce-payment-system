package com.commercepaymentsystem.infrastructure.portone;

public record PortOneAccessTokenResponse(
	String accessToken,
	String refreshToken
) {
}
