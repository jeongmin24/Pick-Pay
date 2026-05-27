package com.ssafy.pickpay.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.pickpay.domain.User;
import com.ssafy.pickpay.repository.UserRepository;


@Component
public class DataInitializer implements CommandLineRunner {

	private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
	public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		super();
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}
	
	@Override
	@Transactional
	public void run(String... args) throws Exception {
		if(userRepository.count() == 0) {
				for (int i = 1; i <= 10; i++) {
	                User user = User.builder()
	                        .loginId("user" + i)
	                        .password(passwordEncoder.encode("1234")) 
	                        .nickname("테스터" + i)
	                        .build();
	                userRepository.save(user);
	            }
				System.out.println("테스트용 유저 데이터 10개 생성 완료");
		}
	}
	
	
    
    
    
}
