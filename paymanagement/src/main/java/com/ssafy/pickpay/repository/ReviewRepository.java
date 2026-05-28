package com.ssafy.pickpay.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ssafy.pickpay.domain.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {
	
	// 리뷰를 단건 조회할 때 작성자(User) 정보를 한 번에 JOIN해서 가져옴.
	@Query("select r from Review r join fetch r.user where r.reviewId = :reviewId")
	Optional<Review> findByIdWithUser(@Param("reviewId") Long reviewId);
	
	// 특정 주문(Order)에 이미 작성된 리뷰가 있는지 확인 (중복 작성 방지용)
	boolean existsByOrderOrderId(Long orderId);

}
