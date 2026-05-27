package com.ssafy.pickpay.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReviewRequestDTO {
	private Long orderId;
	private String content;
	private Integer rating;
	private String imageUrl;
	
	
}
