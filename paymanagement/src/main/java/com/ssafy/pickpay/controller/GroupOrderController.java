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
import com.ssafy.pickpay.dto.GroupOrderCreateResponse;
import com.ssafy.pickpay.dto.ReceiptResponseDTO;
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
        
        // DTO로 변환
        GroupOrderCreateResponse response = new GroupOrderCreateResponse(
        		createdOrder.getGroupId(),
        		createdOrder.getShareLink(),
        		createdOrder.getStatus()
        		);
        return ResponseEntity.ok(response);
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
	public ResponseEntity<ReceiptResponseDTO> getReceipt(@PathVariable Long groupId) {
		
		ReceiptResponseDTO receipt = groupOrderService.getReceipt(groupId);
		
		return ResponseEntity.ok(receipt);
	}

}
