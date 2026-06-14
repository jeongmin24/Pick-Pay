package com.ssafy.pickpay.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record IndividualOrderCreateRequestDTO (
		@NotEmpty(message = "주문 항목이 비어있습니다.") List<@Valid CartItemRequest> items
		) {
	
}