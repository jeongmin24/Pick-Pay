package com.ssafy.pickpay.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public class GroupJoinResponseDTO {
	private String groupId;
	private String status;
	private boolean host;
}

