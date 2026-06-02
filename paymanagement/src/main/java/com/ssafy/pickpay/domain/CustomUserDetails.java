package com.ssafy.pickpay.domain;

import java.util.Collection;
import java.util.Collections;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Getter;


@Getter
public class CustomUserDetails implements UserDetails {
	
	private final Long userId;      // PK
    private final String loginId;   // 로그인용 아이디
    private final String password;
    private final String role;
    
    public CustomUserDetails(User user) {
		super();
		this.userId = user.getUserId();
		this.loginId = user.getLoginId();
		this.password = user.getPassword();
		this.role = "ROLE_USER"; // ROLE_USER
	}
    
    // JWTFilter용 생성자 ( 토큰 검증이 끝난뒤 인증객체를 만드는 단계 ) 
    public CustomUserDetails(Long userId, String loginId, String role) {
		super();
		this.userId = userId;
		this.loginId = loginId;
		this.password = null;
		this.role = role; // ROLE_USER
	}
    
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		// TODO Auto-generated method stub
		return Collections.singletonList(new SimpleGrantedAuthority(role));
	}
	
	@Override
	public String getUsername() {
		// TODO Auto-generated method stub
		return loginId;
	}

	@Override
	public @Nullable String getPassword() {
		// TODO Auto-generated method stub
		return password;
	}

	public CustomUserDetails(Long userId, String loginId, String password, String role) {
		super();
		this.userId = userId;
		this.loginId = loginId;
		this.password = password;
		this.role = role;
	}
	
	
	
    
}
