package com.ssafy.pickpay.dto;

import java.time.LocalDateTime;
import java.util.List;

public record IndividualOrderReceiptResponseDTO (
		Long orderId,
        Long totalPrice,
        String status,
        LocalDateTime createdAt,
        List<OrderReceiptItemDTO> items) {
	
}