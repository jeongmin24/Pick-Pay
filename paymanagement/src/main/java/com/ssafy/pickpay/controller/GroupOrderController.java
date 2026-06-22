package com.ssafy.pickpay.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.pickpay.domain.CustomUserDetails;
import com.ssafy.pickpay.domain.GroupOrder;
import com.ssafy.pickpay.dto.GroupJoinRequestDTO;
import com.ssafy.pickpay.dto.GroupJoinResponseDTO;
import com.ssafy.pickpay.dto.GroupOrderCreateResponse;
import com.ssafy.pickpay.dto.GroupOrderReceiptResponseDTO;
import com.ssafy.pickpay.service.GroupOrderService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/groups")
public class GroupOrderController {
	
	private final GroupOrderService groupOrderService;
	
	// 그룹 주문 방 생성
	@PostMapping
	public ResponseEntity<GroupOrderCreateResponse> createGroup(Authentication authentication) {
        
		// String loginId = authentication.getName(); // authentication에서 userId 받아올수 있음  
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		Long userId = userDetails.getUserId();
		
		// groupOrderService -> Entity 
        GroupOrder createdOrder = groupOrderService.createSession(userId);
        
        String shareLink = "pickpay://group.join?token="+createdOrder.getShareToken();
        
        // DTO로 변환
        GroupOrderCreateResponse response = new GroupOrderCreateResponse(
        		createdOrder.getGroupId(),
        		shareLink,
        		createdOrder.getStatus().name(),
        		true // 방을 생성하면 방장 true 
        		);
        return ResponseEntity.ok(response);
    }
	
	// 링크를 통해 방 입장
	@PostMapping("/join")
	public GroupJoinResponseDTO joinGroup(Authentication authentication, @RequestBody GroupJoinRequestDTO request) {
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		Long userId = userDetails.getUserId();
		
		return groupOrderService.joinGroup(userId, request.getShareToken());
	}
	
	// 방장 주문 마감
	@PatchMapping("/{groupId}/close")
	public ResponseEntity<String> closeGroupOrder(
			@PathVariable Long groupId,
			@RequestParam String payType
			){
		groupOrderService.closeSession(groupId, payType);
		return ResponseEntity.ok("주문이 성공적으로 마감되었습니다.");
	}
	
	// 영수증 조회 api 
	@GetMapping("/{groupId}/receipt")
	public ResponseEntity<GroupOrderReceiptResponseDTO> getReceipt(@PathVariable Long groupId) {
		
		GroupOrderReceiptResponseDTO receipt = groupOrderService.getReceipt(groupId);
		
		return ResponseEntity.ok(receipt);
	}

}
