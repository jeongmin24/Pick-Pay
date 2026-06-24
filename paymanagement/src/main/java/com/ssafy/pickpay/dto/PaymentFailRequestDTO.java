package com.ssafy.pickpay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentFailRequestDTO(
        @NotBlank(message = "orderId is required.")
        String orderId,
        @NotNull(message = "reason is required.")
        String reason
) {
}
