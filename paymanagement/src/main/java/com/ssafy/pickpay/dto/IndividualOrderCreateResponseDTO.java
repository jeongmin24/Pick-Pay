package com.ssafy.pickpay.dto;

public record IndividualOrderCreateResponseDTO (
		Long orderId,
        Long totalPrice,
        String status
		) {
	
}
