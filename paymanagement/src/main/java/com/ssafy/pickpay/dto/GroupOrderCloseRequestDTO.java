package com.ssafy.pickpay.dto;

import com.ssafy.pickpay.common.GroupPayType;

import jakarta.validation.constraints.NotNull;

public record GroupOrderCloseRequestDTO(
		@NotNull(message = "payType은 필수입니다.")
		GroupPayType payType
		) {

}
