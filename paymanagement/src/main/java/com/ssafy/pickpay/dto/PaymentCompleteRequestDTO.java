package com.ssafy.pickpay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentCompleteRequestDTO(
		
		@NotBlank(message = "paymentKey는 필수입니다.")
		String paymentKey,
		@NotNull(message = "orderId는 필수입니다.")
		String orderId, // 실제 값은 OrderNo
		@NotNull
		Long amount
		) {

}
