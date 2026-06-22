package com.ssafy.pickpay.config;

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

    private final Hashids hashids;

	public OrderNoGenerator() {
		this.hashids = new Hashids("pickpay-order-salt-change-this", 8);
	}
    
	public String generate(Long orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("orderId가 없으면 주문번호를 생성할 수 없습니다.");
        }

        String date = LocalDate.now(KOREA_ZONE).format(DATE_FORMATTER);
        String encodedId = hashids.encode(orderId);

        return PREFIX + "-" + date + "-" + encodedId;
    }
	
	// orderNo 복호화 
	public Long decodeOrderId(String orderNo) {
        String[] parts = orderNo.split("-");

        if (parts.length != 3) {
            throw new IllegalArgumentException("올바르지 않은 주문번호 형식입니다.");
        }

        long[] decoded = hashids.decode(parts[2]);

        if (decoded.length == 0) {
            throw new IllegalArgumentException("복호화할 수 없는 주문번호입니다.");
        }

        return decoded[0];
    }
    
}
