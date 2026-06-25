package com.ssafy.pickpay.config;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.hashids.Hashids;
import org.springframework.stereotype.Component;

@Component
public class OrderNoGenerator {
	
	private static final String PREFIX = "ORD";
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final char[] RANDOM_SUFFIX_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final int RANDOM_SUFFIX_LENGTH = 8;

    private final Hashids hashids;
    private final SecureRandom secureRandom;

	public OrderNoGenerator() {
		this.hashids = new Hashids("pickpay-order-salt-change-this", 8);
        this.secureRandom = new SecureRandom();
	}
    
	public String generate(Long orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("orderId is required to generate orderNo.");
        }

        String date = LocalDate.now(KOREA_ZONE).format(DATE_FORMATTER);
        String encodedId = hashids.encode(orderId);
        String randomSuffix = generateRandomSuffix();

        return PREFIX + "-" + date + "-" + encodedId + "-" + randomSuffix;
    }
	
    private String generateRandomSuffix() {
        StringBuilder suffix = new StringBuilder(RANDOM_SUFFIX_LENGTH);

        for (int i = 0; i < RANDOM_SUFFIX_LENGTH; i++) {
            suffix.append(RANDOM_SUFFIX_ALPHABET[secureRandom.nextInt(RANDOM_SUFFIX_ALPHABET.length)]);
        }

        return suffix.toString();
    }

	public Long decodeOrderId(String orderNo) {
        if (orderNo == null || orderNo.isBlank()) {
            throw new IllegalArgumentException("orderNo is required.");
        }

        String[] parts = orderNo.split("-");

        if (parts.length != 3 && parts.length != 4) {
            throw new IllegalArgumentException("Invalid orderNo format.");
        }

        if (!PREFIX.equals(parts[0])) {
            throw new IllegalArgumentException("Invalid orderNo prefix.");
        }

        long[] decoded = hashids.decode(parts[2]);

        if (decoded.length == 0) {
            throw new IllegalArgumentException("Unable to decode orderNo.");
        }

        return decoded[0];
    }
    
}
