package com.ssafy.pickpay.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.pickpay.dto.ReviewRequestDTO;
import com.ssafy.pickpay.dto.ReviewResponseDTO;
import com.ssafy.pickpay.service.ReviewService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {
	private final ReviewService reviewService;
	
	@PostMapping
	public ResponseEntity<Long> createReview(@RequestHeader("X-USER-ID") Long userId,
			@RequestBody ReviewRequestDTO request) {
		return ResponseEntity.ok(reviewService.createReview(userId, request));
	}
	
	@GetMapping("/{reviewId}")
	public ResponseEntity<ReviewResponseDTO> getReview(@PathVariable("reviewId") Long reivewId) {
		return ResponseEntity.ok(reviewService.getReviewDetails(reivewId));
	}
	
	@GetMapping
	public ResponseEntity<List<ReviewResponseDTO>> getAllReviews() {
		return ResponseEntity.ok(reviewService.getAllReviews());
	}
}
