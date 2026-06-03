package com.commercepaymentsystem.domain.order.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class OrderNumberGenerator {

	private static final DateTimeFormatter FORMATTER =
		DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

	public String generate() {
		String timestamp = LocalDateTime.now().format(FORMATTER);
		String suffix = UUID.randomUUID()
			.toString()
			.substring(0, 8)
			.toUpperCase();

		return "ORD-" + timestamp + "-" + suffix;
	}
}