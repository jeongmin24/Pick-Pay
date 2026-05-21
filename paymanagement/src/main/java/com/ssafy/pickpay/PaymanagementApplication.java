package com.ssafy.pickpay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class PaymanagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaymanagementApplication.class, args);
	}

}
