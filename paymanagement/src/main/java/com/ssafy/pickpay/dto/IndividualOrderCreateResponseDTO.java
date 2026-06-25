package com.ssafy.pickpay.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record IndividualOrderCreateResponseDTO(
        @Schema(description = "외부 결제용 주문 ID. DB 내부 PK가 아니라 orderNo 값입니다.")
        String orderId,
        @Schema(description = "고객 화면 및 CS용 표시 주문번호. 매일 001부터 다시 시작합니다.")
        String displayOrderNo,
        Long totalPrice,
        String status
) {
}
