package com.ssafy.pickpay.domain;

import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.ssafy.pickpay.dto.UserRequestDTO;

import jakarta.annotation.Generated;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;

@Entity
@Getter @Setter @Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users") // 'user'는 일부 DB 예약어일 수 있으므로 복수형 권장
@EntityListeners(AuditingEntityListener.class)
public class User {


	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;
    
    @Column(unique = true, nullable = false)
    private String loginId; // 로그인용 아이디 

    private String password;
    
    @Column(length = 100)
    private String nickname;

    private String fcmToken;
    
    @CreationTimestamp // INSERT 시 자동으로 현재 시간 저장
    private LocalDateTime createdAt;
    
    public void updateUser(UserRequestDTO dto) {
    	this.nickname = dto.getNickname();
    }

    private String imageUrl;


}