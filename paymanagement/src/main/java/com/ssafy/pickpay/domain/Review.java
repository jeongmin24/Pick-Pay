package com.ssafy.pickpay.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Getter 
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(columnDefinition = "TEXT")
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(columnDefinition = "TEXT")
    private String content;

    private Integer rating;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public Review(String imageUrl, User user, Order order, String content, Integer rating) {
        this.imageUrl = imageUrl;
        this.user = user;
        this.order = order;
        this.content = content;
        this.rating = rating;
    }

    private Review(Builder builder) {
        this.imageUrl = builder.imageUrl;
        this.user = builder.user;
        this.order = builder.order;
        this.content = builder.content;
        this.rating = builder.rating;
    }

    public void updateReview(String content, Integer rating, String imageUrl) {
        this.content = content;
        this.rating = rating;
        this.imageUrl = imageUrl;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String imageUrl;
        private User user;
        private Order order;
        private String content;
        private Integer rating;

        private Builder() {
        }

        public Builder imageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        public Builder user(User user) {
            this.user = user;
            return this;
        }

        public Builder order(Order order) {
            this.order = order;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder rating(Integer rating) {
            this.rating = rating;
            return this;
        }

        public Review build() {
            return new Review(this);
        }
    }
}