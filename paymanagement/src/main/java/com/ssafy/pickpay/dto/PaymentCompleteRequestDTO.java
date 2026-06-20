package com.ssafy.pickpay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentCompleteRequestDTO(
		
		@NotBlank
		String paymentKey,
		@NotNull
		Long orderId,
		@NotNull
		Long amount
		) {

}
