package com.ssafy.pickpay.dto;

import java.time.LocalDateTime;

import com.ssafy.pickpay.domain.Review;

import lombok.Getter;

@Getter
public class ReviewResponseDTO {
	private final Long reviewId;
	private final String content;
	private final Integer rating;
	private final String imageUrl;
	private final LocalDateTime createdAt;
	
	private final Long userId;
	private final String nickname;
	private final String profileUrl;
	
	public ReviewResponseDTO(Review review) {
		this.reviewId = review.getReviewId();
        this.content = review.getContent();
        this.rating = review.getRating();
        this.imageUrl = review.getImageUrl();
        this.createdAt = review.getCreatedAt();

        this.userId = review.getUser().getUserId();
        this.nickname = review.getUser().getNickname();
        this.profileUrl = review.getUser().getFcmToken();
	}
}
