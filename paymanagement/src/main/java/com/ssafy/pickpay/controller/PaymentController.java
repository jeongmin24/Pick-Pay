package com.ssafy.pickpay.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.pickpay.domain.CustomUserDetails;
import com.ssafy.pickpay.dto.PaymentCompleteRequestDTO;
import com.ssafy.pickpay.dto.PaymentCompleteResponseDTO;
import com.ssafy.pickpay.service.PaymentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {
	
	private final PaymentService paymentService;
	
	@PostMapping("/complete")
	public ResponseEntity<PaymentCompleteResponseDTO> completePayment(
			Authentication authentication, @Valid @RequestBody PaymentCompleteRequestDTO request
			) {
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		Long userId = userDetails.getUserId();

		log.info(
				"Payment complete request userId={}, paymentKey={}, orderId={}, amount={}",
				userId,
				maskPaymentKey(request.paymentKey()),
				request.orderId(),
				request.amount()
		);
		
		PaymentCompleteResponseDTO response = paymentService.completePayment(userId, request);
		
		return ResponseEntity.ok(response);
	}

	private String maskPaymentKey(String paymentKey) {
		if (paymentKey == null || paymentKey.length() <= 12) {
			return "***";
		}
		return paymentKey.substring(0, 6) + "..." + paymentKey.substring(paymentKey.length() - 4);
	}

}
