package com.ssafy.pickpay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PaymentCompleteRequestDTO(
		
		@NotBlank(message = "paymentKey는 필수입니다.")
		String paymentKey,
		@NotNull(message = "orderId는 필수입니다.")
		String orderId, // 실제 값은 OrderNo
		@NotNull
		@Positive(message = "amount는 1 이상이어야 합니다.")
		Long amount
		) {

}
