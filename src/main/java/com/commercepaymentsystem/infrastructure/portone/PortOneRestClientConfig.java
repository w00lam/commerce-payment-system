package com.commercepaymentsystem.infrastructure.portone;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(PortOneProperties.class)
public class PortOneRestClientConfig {

	@Bean
	public RestClient portOneRestClient(PortOneProperties properties) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(properties.connectTimeout());
		requestFactory.setReadTimeout(properties.readTimeout());

		return RestClient.builder()
			.baseUrl(properties.baseUrl())
			.requestFactory(requestFactory)
			.defaultHeaders(headers -> {
				headers.setContentType(MediaType.APPLICATION_JSON);
				headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
			})
			.build();
	}
}
