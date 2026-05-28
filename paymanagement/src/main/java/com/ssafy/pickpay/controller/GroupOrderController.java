package com.ssafy.pickpay.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.pickpay.domain.GroupOrder;
import com.ssafy.pickpay.dto.GroupOrderRequestDTO;
import com.ssafy.pickpay.service.GroupOrderService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/groups")
public class GroupOrderController {
	
	private final GroupOrderService groupOrderService;
	
	// 그룹 주문 방 생성
	@PostMapping
	public ResponseEntity<GroupOrder> createGroup(Authentication authentication) {
        
		String loginId = authentication.getName(); 
		
		// 서비스 로직 호출 (RDB 저장 및 Firebase 노드 생성)
        GroupOrder createdOrder = groupOrderService.createSession(loginId);
        
        // 200 OK와 함께 생성된 방 정보 반환
        return ResponseEntity.ok(createdOrder);
    }

}
