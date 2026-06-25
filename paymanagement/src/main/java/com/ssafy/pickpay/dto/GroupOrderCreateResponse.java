package com.ssafy.pickpay.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 방생성 응답DTO
@Getter @AllArgsConstructor
public class GroupOrderCreateResponse {
	private String groupId;
	private String shareLink;
	private String status;
	private boolean host;
}
