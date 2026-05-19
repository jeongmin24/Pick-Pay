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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users") // 'user'는 일부 DB 예약어일 수 있으므로 복수형 권장
@EntityListeners(AuditingEntityListener.class)
public class User {

    protected User() {
		super();
	}

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

    public Long getUserId() {
		return userId;
	}


	public String getLoginId() {
		return loginId;
	}


	public String getPassword() {
		return password;
	}


	public String getNickname() {
		return nickname;
	}


	public String getFcmToken() {
		return fcmToken;
	}


	public LocalDateTime getCreatedAt() {
		return createdAt;
	}



	public User(String loginId, String password, String nickname, String fcmToken) {
		super();
		this.loginId = loginId;
		this.password = password;
		this.nickname = nickname;
		this.fcmToken = fcmToken;
	}
    

	private User(Builder builder) {
		this.loginId = builder.loginId;
		this.password = builder.password;
		this.nickname = builder.nickname;
		this.fcmToken = builder.fcmToken;
	}


	public static Builder builder() {
		return new Builder();
	}

	// builder에서 직접 값을 넣어야 하는 필드만 체크
	public static final class Builder {
		private String loginId;
		private String password;
		private String nickname;
		private String fcmToken;

		private Builder() {
		}


		public Builder loginId(String loginId) {
			this.loginId = loginId;
			return this;
		}

		public Builder password(String password) {
			this.password = password;
			return this;
		}

		public Builder nickname(String nickname) {
			this.nickname = nickname;
			return this;
		}

		public Builder fcmToken(String fcmToken) {
			this.fcmToken = fcmToken;
			return this;
		}


		public String getLoginId() {
			return loginId;
		}


		public String getPassword() {
			return password;
		}


		public String getNickname() {
			return nickname;
		}


		public String getFcmToken() {
			return fcmToken;
		}


		public User build() {
			return new User(this);
		}
	}
	

}