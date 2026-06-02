package com.ssafy.pickpay.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public class GroupOrderCreateResponse {
	private Long groupId;
	private String shareLink;
	private String status;
}
