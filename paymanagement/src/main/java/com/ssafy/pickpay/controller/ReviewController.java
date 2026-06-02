package com.ssafy.pickpay.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ssafy.pickpay.dto.ReviewRequestDTO;
import com.ssafy.pickpay.dto.ReviewResponseDTO;
import com.ssafy.pickpay.service.ReviewService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {
	private final ReviewService reviewService;
	
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Long> createReview(
			Authentication authentication,
			@RequestParam("content") String content,
			@RequestParam("rating") String rating,
			@RequestPart(value = "image", required = false) MultipartFile image,
			HttpServletRequest request) {
		
		String cleanRating = rating.replace("\"", "");
		int numericRating = Integer.parseInt(cleanRating);
		
		String baseUrl = request.getRequestURL().toString().replace(request.getRequestURI(), "");
		return ResponseEntity.ok(reviewService.createReview(1L, content, numericRating, image, baseUrl));
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
