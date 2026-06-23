package com.ssafy.pickpay.dto;

import java.util.List;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @Builder
@NoArgsConstructor @AllArgsConstructor
public class GroupOrderReceiptResponseDTO {
	
	private String groupId;
	private String payType;
	private Long totalGroupPrice;
	private List<UserReceiptDTO> userReceipts;
	
	// 유저별 영수증 inner class
	@Getter @Builder
	@NoArgsConstructor @AllArgsConstructor
	public static class UserReceiptDTO {
		private Long userId;
		private String orderNo;
		private String nickname;
		private Long userTotalPrice;
		private List<OrderReceiptItemDTO> items; // 사용자가 시킨 메뉴들
	}
	

}
