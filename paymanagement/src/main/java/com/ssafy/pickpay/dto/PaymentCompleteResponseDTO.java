package com.ssafy.pickpay.dto;

public record PaymentCompleteResponseDTO(
		Long orderId,
		Long amount,
		String orderStatus,
		String message
		) {

}
