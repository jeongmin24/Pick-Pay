package com.ssafy.pickpay.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class FirebaseCartItemDTO {
	private Long userId;
    private Long productId; // Menu PK 
    private Integer quantity;
}
