package com.miniproject.server.domain.jwt.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "jwt_refresh_entity")
@Getter @Builder
@NoArgsConstructor @AllArgsConstructor
public class RefreshEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 자동 생성(채번)되는 refresh 토큰 id

    @Column(name = "username", nullable = false)
    private String username; // 발급한 계정의 username

    @Column(name = "refresh", nullable = false, length = 512)
    private String refresh; // 발급한 리프레시 토큰

    @CreatedDate
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate; // 생성 시간


}
