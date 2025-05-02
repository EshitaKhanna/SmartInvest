package com.example.investmentplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.example.investmentplatform.repository")
@EntityScan(basePackages = "com.example.investmentplatform.entity")
@EnableScheduling
public class InvestmentPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(InvestmentPlatformApplication.class, args);
	}

}
