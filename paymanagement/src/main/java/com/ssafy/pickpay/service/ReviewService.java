package com.ssafy.pickpay.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.pickpay.domain.Review;
import com.ssafy.pickpay.domain.User;
import com.ssafy.pickpay.dto.ReviewRequestDTO;
import com.ssafy.pickpay.dto.ReviewResponseDTO;
import com.ssafy.pickpay.repository.ReviewRepository;
import com.ssafy.pickpay.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {
	
	private final ReviewRepository reviewRepository;
	private final UserRepository userRepository;
	
	@Transactional
    public Long createReview(Long userId, ReviewRequestDTO request) {
        // 중복 리뷰 검증
        if (reviewRepository.existsByOrderOrderId(request.getOrderId())) {
            throw new IllegalStateException("이미 해당 주문에 대한 리뷰가 존재합니다.");
        }

        // 유저 및 주문 엔티티 조회 (실제로는 각 Repository에서 꺼내옴)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
        
        // 작성하신 순수 자바 직접 빌더 패턴 활용
        Review review = Review.builder()
                .user(user)
                .content(request.getContent())
                .rating(request.getRating())
                .imageUrl(request.getImageUrl())
                .build();

        return reviewRepository.save(review).getReviewId();
    }
	
	public ReviewResponseDTO getReviewDetails(Long reviewId) {
		Review review = reviewRepository.findById(reviewId)
				.orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));
		
		return new ReviewResponseDTO(review);
	}
	
	public List<ReviewResponseDTO> getAllReviews() {
		List<Review> reviews = reviewRepository.findAllWithUser();
		
		return reviews.stream()
	            .map(ReviewResponseDTO::new) // 각 Review 엔티티를 ReviewResponseDTO로 변환 (new ReviewResponseDTO(review))
	            .toList();
	}
	
}
