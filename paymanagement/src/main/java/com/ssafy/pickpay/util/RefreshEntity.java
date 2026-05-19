package com.ssafy.pickpay.util;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "jwt_refresh_entity")
public class RefreshEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loginId", nullable = false)
    private String loginId;

    @Column(name = "refresh", nullable = false, length = 512)
    private String refresh;

    @CreatedDate
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;
    
    private RefreshEntity(Builder builder) {
        this.id = builder.id;
        this.loginId = builder.loginId;
        this.refresh = builder.refresh;
        this.createdDate = builder.createdDate;
    }

	public RefreshEntity(Long id, String loginId, String refresh, LocalDateTime createdDate) {
		super();
		this.id = id;
		this.loginId = loginId;
		this.refresh = refresh;
		this.createdDate = createdDate;
	}

	public RefreshEntity() {
		super();
	}

	public RefreshEntity(String loginId, String refresh) {
		super();
		this.loginId = loginId;
		this.refresh = refresh;
	}

	public Long getId() {
		return id;
	}

	public String getLoginId() {
		return loginId;
	}

	public String getRefresh() {
		return refresh;
	}

	public LocalDateTime getCreatedDate() {
		return createdDate;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setLoginId(String loginId) {
		this.loginId = loginId;
	}

	public void setRefresh(String refresh) {
		this.refresh = refresh;
	}

	public void setCreatedDate(LocalDateTime createdDate) {
		this.createdDate = createdDate;
	}
	
	public static Builder builder() {
        // 내부 static 클래스인 Builder를 생성해서 리턴
        return new Builder(); 
    }
	
	public static class Builder {
        private Long id;
        private String loginId;
        private String refresh;
        private LocalDateTime createdDate;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder loginId(String loginId) {
            this.loginId = loginId;
            return this;
        }

        public Builder refresh(String refresh) {
            this.refresh = refresh;
            return this;
        }

        public Builder createdDate(LocalDateTime createdDate) {
            this.createdDate = createdDate;
            return this;
        }

        public RefreshEntity build() {
            return new RefreshEntity(this);
        }
    }
	

}