package com.ssafy.pickpay.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CloseOrderRequestDTO {
	private String payType; // 마감시 결제방식 입력 
}
