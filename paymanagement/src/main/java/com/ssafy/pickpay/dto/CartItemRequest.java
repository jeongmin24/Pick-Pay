package com.ssafy.pickpay.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CartItemRequest(
		@NotNull(message = "menuId는 필수입니다.")
		Long menuId,
		
		@NotNull(message = "quantity는 필수입니다.")
        @Positive(message = "quantity는 1 이상이어야 합니다.")
		Integer quantity
		) {
	
}