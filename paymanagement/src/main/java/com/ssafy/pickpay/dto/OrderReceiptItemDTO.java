package com.ssafy.pickpay.dto;

public record OrderReceiptItemDTO(
        String menuName,
        Integer quantity,
        Long price
) {
}