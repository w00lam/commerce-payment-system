package com.commercepaymentsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CommercePaymentSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(CommercePaymentSystemApplication.class, args);
	}

}
