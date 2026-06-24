package com.ssafy.pickpay.dto;

import java.time.LocalDateTime;

public record RecentOrderResponseDTO(
        String orderNo,
        Long totalPrice,
        String status,
        LocalDateTime createdAt,
        String firstMenuName,
        Integer totalQuantity,
        Integer itemCount
) {
}
