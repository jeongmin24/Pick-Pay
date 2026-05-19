package com.ssafy.pickpay.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ssafy.pickpay.dao.UserDao;
import com.ssafy.pickpay.domain.User;
import com.ssafy.pickpay.dto.UserRequestDTO;

import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService implements UserDetailsService {
	
	private final UserDao userRepository;
	private final PasswordEncoder passwordEncoder;

	public UserService(UserDao userRepository, PasswordEncoder passwordEncoder) {
		super();
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}
	
	// 자체 로그인 회원 가입 (존재 여부)
	public Boolean existUser(UserRequestDTO dto) {
		return userRepository.existsByLoginId(dto.getLoginId());
	}

    // 자체 로그인 회원 가입
	@Transactional
	public Long addUser(UserRequestDTO dto) {
		
		if(userRepository.existsByLoginId(dto.getLoginId())) {
			throw new IllegalArgumentException("이미 유저가 존재합니다.");
		}
		
		User user = User.builder()
				.loginId(dto.getLoginId())
				.password(passwordEncoder.encode(dto.getPassword()))
				.nickname(dto.getNickname())
				.build();
					
		return userRepository.save(user).getUserId(); //save: insert or update
		
	}
	

    // 자체 로그인
	@Transactional(readOnly = true)
	@Override
	public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
		User user = userRepository.findByLoginId(loginId)
				.orElseThrow(()-> new UsernameNotFoundException(loginId));
		
		return org.springframework.security.core.userdetails.User.builder()
				.username(user.getLoginId()) // 스프링시큐리티의 경우 계정 식별자는 username 
				.password(user.getPassword())
				.roles("USER") // ROLE_USER -> authentication 용! 
				.build();
	}
	
	
    // 자체 로그인 회원 정보 수정
	@Transactional
	public Long updateUser(UserRequestDTO dto) throws AccessDeniedException {
		
		String sessionLoginId = SecurityContextHolder.getContext().getAuthentication().getName();
		// getName: 현재 로그인된 사용자의 username 값 
		
		if(!sessionLoginId.equals(dto.getLoginId())) {
			throw new AccessDeniedException("본인 계정만 수정 가능"); //403
		}
		
		User user = userRepository.findByLoginId(dto.getLoginId())
				.orElseThrow(() -> new UsernameNotFoundException(dto.getLoginId()));
		
		user.updateUser(dto);
		
		return userRepository.save(user).getUserId();
	}

	
	// 자체/소셜 유저 정보 조회

}
