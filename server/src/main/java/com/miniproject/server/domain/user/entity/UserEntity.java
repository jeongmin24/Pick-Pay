package com.miniproject.server.domain.user.entity;

import com.miniproject.server.domain.user.dto.UserRequestDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@EntityListeners(AuditingEntityListener.class) // 엔티티가 생성, 수정되면 생성일, 최종 수정일이 자동 등록
@Table(name = "user_user_entity")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {

    // @Id: 엔티티의 기본키
    // @GenerateValue: id값을 자동으로 생성하도록 JPA에 알려주는 설정
    // strategy는 어떻게 id값을 생성할지 지정, GenerateType.IDENTITY는 DB의 자동 증가 기능을 사용(id 값을 직접 지정하지 않아도 자동으로 채워준다)
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // updateable: 한번 username이 결정되면 수정할 수 없다
    @Column(name = "username", unique = true, nullable = false, updatable = false)
    private String username; // 로그인용 아이디

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "is_lock", nullable = false)
    private Boolean isLock; // 계정이 잠겼는지 잠기지 않았는지 체크

    @Column(name = "is_social", nullable = false)
    private Boolean isSocial; // 이 계정이 소셜로그인인지 자체로그인인지

    // 자체로그인인 경우 null값이 들어가므로 nullable = false 하면 안됨
    // 첫번째 값부터 integer 값으로 들어가기 때문에 EnumType.STRING으로 지정한 Enum이 들어갈 수 있도록 세팅
    // -> Google, Naver를 인덱스 번호로 DB에 저장하지 않고 문자열 그대로 저장하는것
    @Enumerated(EnumType.STRING)
    @Column(name = "social_provider_type")
    private SocialProviderType socialProviderType; // Google? Naver?

    @Enumerated(EnumType.STRING)
    @Column(name = "role_type", nullable = false)
    private UserRoleType roleType; // Spring Security에서 사용할 User의 Role값 Admin? User?

    @Column(name = "nickname")
    private String nickname;

    @Column(name = "email")
    private String email;

    @CreatedDate
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    @LastModifiedDate
    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    // @Setter 대신 수정 메서드
    // 세터의 경우 사용을 지양
    public void updateUser(UserRequestDto dto) {
        this.email = dto.getEmail();
        this.nickname = dto.getNickname();
    }
}
