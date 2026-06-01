package com.commercepaymentsystem.infrastructure.portone;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "portone")
public record PortOneProperties(
	String baseUrl,
	String apiSecret,
	Duration connectTimeout,
	Duration readTimeout
) {

	private static final String DEFAULT_BASE_URL = "https://api.portone.io";
	private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(2);
	private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(5);

	public PortOneProperties {
		if (isBlank(baseUrl)) {
			baseUrl = DEFAULT_BASE_URL;
		}
		if (connectTimeout == null) {
			connectTimeout = DEFAULT_CONNECT_TIMEOUT;
		}
		if (readTimeout == null) {
			readTimeout = DEFAULT_READ_TIMEOUT;
		}
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
