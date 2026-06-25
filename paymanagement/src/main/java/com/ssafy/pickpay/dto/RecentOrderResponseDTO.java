package com.ssafy.pickpay.dto;

import java.time.LocalDateTime;

public record RecentOrderResponseDTO(
        String orderNo,
        String displayOrderNo,
        Long totalPrice,
        String status,
        LocalDateTime createdAt,
        String firstMenuName,
        Integer totalQuantity,
        Integer itemCount
) {
}
