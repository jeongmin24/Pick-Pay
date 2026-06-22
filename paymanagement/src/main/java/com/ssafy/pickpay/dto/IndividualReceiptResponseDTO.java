package com.ssafy.pickpay.dto;

import java.time.LocalDateTime;
import java.util.List;


public record IndividualReceiptResponseDTO (
		Long orderId,
        Long totalPrice,
        String status,
        LocalDateTime orderedAt,
        List<OrderReceiptItemDTO> items
		) {
	
}
