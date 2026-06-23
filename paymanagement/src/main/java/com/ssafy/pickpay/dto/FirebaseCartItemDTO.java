package com.ssafy.pickpay.dto;

import com.google.firebase.database.IgnoreExtraProperties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@IgnoreExtraProperties
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class FirebaseCartItemDTO {
	private Long userId;
	private String menuName;
    private Long productId; // Menu PK 
    private Integer quantity;
}
