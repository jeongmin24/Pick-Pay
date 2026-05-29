package com.ssafy.pickpay.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.ssafy.pickpay.domain.CustomUserDetails;
import com.ssafy.pickpay.domain.User;
import com.ssafy.pickpay.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService{
	
	private final UserRepository userRepository;
	
	// 자체 로그인
	@Override
	public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
		// TODO Auto-generated method stub
		User user = userRepository.findByLoginId(loginId)
				.orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 사용자 입니다"));
		
		return new CustomUserDetails(user);
	}

}
