package com.ssafy.pickpay.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.pickpay.domain.CustomUserDetails;
import com.ssafy.pickpay.dto.IndividualOrderCreateRequestDTO;
import com.ssafy.pickpay.dto.IndividualOrderCreateResponseDTO;
import com.ssafy.pickpay.dto.IndividualOrderReceiptResponseDTO;
import com.ssafy.pickpay.dto.RecentOrderResponseDTO;
import com.ssafy.pickpay.service.OrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class IndividualOrderController {
	
	private final OrderService orderService;
	
	@PostMapping
	public ResponseEntity<IndividualOrderCreateResponseDTO> createIndividualOrder(
			Authentication authentication, 
			@Valid @RequestBody IndividualOrderCreateRequestDTO request
			) {
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		Long userId = userDetails.getUserId();
		
		IndividualOrderCreateResponseDTO response = orderService.createIndividualOrder(userId, request);
		
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/recent")
	public ResponseEntity<List<RecentOrderResponseDTO>> getRecentOrders(
			Authentication authentication,
			@RequestParam(defaultValue = "10") int limit
			) {
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		Long userId = userDetails.getUserId();

		List<RecentOrderResponseDTO> response = orderService.getRecentOrders(userId, limit);

		return ResponseEntity.ok(response);
	}
	
	@GetMapping("/{orderNo}/receipt")
	public ResponseEntity<IndividualOrderReceiptResponseDTO> getIndividualReceipt(
			Authentication authentication,
			@PathVariable String orderNo
			) {
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		Long userId = userDetails.getUserId();
		
		IndividualOrderReceiptResponseDTO response = orderService.getIndividualReceipt(userId, orderNo);
		
		return ResponseEntity.ok(response);
	}

}
