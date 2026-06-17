package com.ssafy.pickpay.service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
	
	private final String uploadDir = "./upload/reviews/";

	@Transactional
	public Long createReview(Long userId, String content, int rating, MultipartFile image, String baseUrl) {
		// 유저 및 주문 엔티티 조회 (실제로는 각 Repository에서 꺼내옴)
		User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

		String savedImageUrl = null;
		
		if(image != null && !image.isEmpty()) {
			try {
				File dir = new File(uploadDir);
				if(!dir.exists()) dir.mkdirs();
				
				String originalFilename = image.getOriginalFilename();
				String extension = "";
				if (originalFilename != null && originalFilename.contains(".")) {
					extension = originalFilename.substring(originalFilename.lastIndexOf("."));
				}
				String savedFilename = UUID.randomUUID().toString() + extension;

				// 하드디스크 디렉토리에 바이너리 파일 복사
				File targetFile = new File(dir.getAbsolutePath() + File.separator + savedFilename);
				image.transferTo(targetFile);

				// 안드로이드 앱 에뮬레이터가 다운로드할 가상 정적 Web URL 주소 조립
				savedImageUrl = baseUrl + "/images/" + savedFilename;
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("서버 물리 디스크에 파일 저장 실패", e);
			}
		}

		Review review = Review.builder()
				.user(user)
				.content(content)
				.rating(rating)
				.imageUrl(savedImageUrl)
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

		return reviews.stream().map(ReviewResponseDTO::new) // 각 Review 엔티티를 ReviewResponseDTO로 변환 (new
															// ReviewResponseDTO(review))
				.toList();
	}

}
