package com.ssafy.pickpay.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record IndividualOrderCreateResponseDTO (
		@Schema(description = "외부 결제용 주문 ID. DB 내부 PK가 아니라 orderNo 값입니다.")
		String orderId, // 실제 값은 OrderNo
        Long totalPrice,
        String status
		) {
	
}
