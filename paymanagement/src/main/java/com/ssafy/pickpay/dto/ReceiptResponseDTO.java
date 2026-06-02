package com.ssafy.pickpay.dto;

import java.util.List;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Builder
@NoArgsConstructor @AllArgsConstructor
public class ReceiptResponseDTO {
	
	private Long groupId;
	private String payType;
	private Long totalGroupPrice;
	private List<UserReceiptDTO> userReceipts;
	
	// 유저별 영수증 inner class
	@Getter @Builder
	@NoArgsConstructor @AllArgsConstructor
	public static class UserReceiptDTO {
		private Long userId;
		private String nickname;
		private Long userTotalPrice;
		private List<OrderItemDTO> items; // 사용자가 시킨 메뉴들
	}
	
	@Getter @Builder
	@NoArgsConstructor @AllArgsConstructor
	public static class OrderItemDTO {
		private String menuName;
		private int quantity;
		private Long price;
	}

}
