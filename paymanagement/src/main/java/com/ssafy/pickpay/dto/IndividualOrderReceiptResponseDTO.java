package com.ssafy.pickpay.dto;

import java.time.LocalDateTime;
import java.util.List;

public record IndividualOrderReceiptResponseDTO(
        Long orderId,
        String displayOrderNo,
        Long totalPrice,
        String status,
        LocalDateTime createdAt,
        List<OrderReceiptItemDTO> items
) {
}
