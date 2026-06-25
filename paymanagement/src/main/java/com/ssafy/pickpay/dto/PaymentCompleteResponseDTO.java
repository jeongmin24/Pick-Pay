package com.ssafy.pickpay.dto;

public record PaymentCompleteResponseDTO(
        String orderId,
        Long amount,
        String orderStatus,
        String groupId,
        String groupStatus,
        String message
) {
}
