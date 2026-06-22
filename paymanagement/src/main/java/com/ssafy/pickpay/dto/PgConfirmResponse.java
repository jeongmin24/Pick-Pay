package com.ssafy.pickpay.dto;

public record PgConfirmResponse(
		String paymentKey,
        String orderId,
        Long totalAmount,
        String status
		) {

}
