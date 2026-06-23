package com.ssafy.pickpay.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ssafy.pickpay.domain.User;
import com.ssafy.pickpay.dto.UserRequestDTO;
import com.ssafy.pickpay.dto.UserResponseDTO;
import com.ssafy.pickpay.repository.UserRepository;

import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService{
	
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
		super();
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
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
	

//    // 자체 로그인
//	@Transactional(readOnly = true)
//	@Override
//	public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
//		User user = userRepository.findByLoginId(loginId)
//				.orElseThrow(()-> new UsernameNotFoundException(loginId));
//		
//		return org.springframework.security.core.userdetails.User.builder()
//				.username(user.getLoginId()) // 스프링시큐리티의 경우 계정 식별자는 username 
//				.password(user.getPassword())
//				.roles("USER") // ROLE_USER -> authentication 용! 
//				.build();
//	}
//	
	
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

	
	// 자체 유저 정보 조회
	@Transactional(readOnly = true)
	public UserResponseDTO readUser() {
		String loginId = SecurityContextHolder.getContext().getAuthentication().getName();
		
		User user = userRepository.findByLoginId(loginId)
				.orElseThrow(() -> new UsernameNotFoundException("해당 유저를 찾을 수 없습니다." + loginId));
		
		return new UserResponseDTO(loginId, user.getNickname());
	}
	
	// 자체 로그인 회원 탈퇴
	@Transactional
	public void deleteUser(UserRequestDTO dto) throws AccessDeniedException {
		
		SecurityContext context = SecurityContextHolder.getContext();
		String sessionLoginId = context.getAuthentication().getName();
		String sessionRole = context.getAuthentication().getAuthorities().iterator().next().getAuthority();
		
		boolean isOwner = sessionLoginId.equals(dto.getLoginId());
		
		if(!isOwner) {
			throw new AccessDeniedException("본인만 삭제할 수 있습니다");
		}
		
		// 유저 제거 
		userRepository.deleteByLoginId(dto.getLoginId());
		// 리프레시 토큰 제거 
		jwtService.removeRefreshUser(dto.getLoginId());
		
	}
	
	@Transactional
	public void updateFcmToken(Long userId, String fcmToken) {
		userRepository.clearFcmTokenFromOtherUsers(fcmToken, userId);

	    User user = userRepository.findById(userId).orElseThrow();
	    user.setFcmToken(fcmToken);
	}

}
