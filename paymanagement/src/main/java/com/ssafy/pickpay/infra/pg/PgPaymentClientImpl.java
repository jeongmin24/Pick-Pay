package com.ssafy.pickpay.infra.pg;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.ssafy.pickpay.dto.PgConfirmResponse;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PgPaymentClientImpl implements PgPaymentClient {
	
	private final RestClient restClient = RestClient.create();

	@Value("${pg.secret-key}")
	private String secretKey;
	
	@Value("${pg.confirm-url}")
	private String confirmUrl;
	
	@Override
	public PgConfirmResponse confirmPayment(String paymentKey, String orderId, Long amount) {

		String encodedSecretKey = Base64.getEncoder()
				.encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
		
		return restClient.post()
				.uri(confirmUrl)
				.header("Authorization", "Basic " + encodedSecretKey)
                .header("Content-Type", "application/json")
                .body(Map.of(
                        "paymentKey", paymentKey,
                        "orderId", orderId,
                        "amount", amount
                ))
                .retrieve()
                .body(PgConfirmResponse.class);
	}

}
