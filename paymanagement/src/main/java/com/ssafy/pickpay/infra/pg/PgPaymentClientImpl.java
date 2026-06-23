package com.ssafy.pickpay.infra.pg;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.ssafy.pickpay.dto.PgConfirmResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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

		log.info(
				"Toss confirm request paymentKey={}, orderId={}, amount={}",
				maskPaymentKey(paymentKey),
				orderId,
				amount
		);
		
		try {
			PgConfirmResponse response = restClient.post()
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

			log.info(
					"Toss confirm success paymentKey={}, orderId={}, totalAmount={}, status={}",
					response != null ? maskPaymentKey(response.paymentKey()) : null,
					response != null ? response.orderId() : null,
					response != null ? response.totalAmount() : null,
					response != null ? response.status() : null
			);

			return response;
		} catch (RestClientResponseException e) {
			log.error(
					"Toss confirm failed status={}, body={}",
					e.getStatusCode(),
					e.getResponseBodyAsString(),
					e
			);
			throw e;
		}
	}

	private String maskPaymentKey(String paymentKey) {
		if (paymentKey == null || paymentKey.length() <= 12) {
			return "***";
		}
		return paymentKey.substring(0, 6) + "..." + paymentKey.substring(paymentKey.length() - 4);
	}

}
