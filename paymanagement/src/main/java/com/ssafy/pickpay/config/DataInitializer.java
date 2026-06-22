package com.ssafy.pickpay.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.pickpay.domain.Menu;
import com.ssafy.pickpay.domain.Review;
import com.ssafy.pickpay.domain.User;
import com.ssafy.pickpay.repository.MenuRepository;
import com.ssafy.pickpay.repository.ReviewRepository;
import com.ssafy.pickpay.repository.UserRepository;


@Component
public class DataInitializer implements CommandLineRunner {

	private final UserRepository userRepository;
	private final ReviewRepository reviewRepository;
	private final MenuRepository menuRepository;
    private final PasswordEncoder passwordEncoder;
    
	public DataInitializer(UserRepository userRepository, ReviewRepository reviewRepository, PasswordEncoder passwordEncoder, MenuRepository menuRepository) {
		super();
		this.userRepository = userRepository;
		this.reviewRepository = reviewRepository;
		this.menuRepository = menuRepository;
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
	                        .imageUrl("https://www.google.com/images/branding/googlelogo/2x/googlelogo_color_272x92dp.png")
	                        .build();
	                userRepository.save(user);
	            }
				System.out.println("테스트용 유저 데이터 10개 생성 완료");
		}
		
		// 2. 리뷰 테스트 데이터 생성 (유저 ID 1~5번을 그대로 활용 ⭕)
				if (reviewRepository.count() == 0) {
					for (long i = 1; i <= 5; i++) {
						// 💡 핵심: DB에 저장된 1번부터 5번 유저를 ID로 직접 찾아서 가져옵니다.
						User writer = userRepository.findById(i)
								.orElseThrow(() -> new IllegalStateException("테스트 유저를 찾을 수 없습니다."));

						Review review = Review.builder()
								.user(writer) // 찾아온 유저 객체를 리뷰에 매핑
								.content("테스터" + i + "님이 작성한 가짜 리뷰입니다. 아주 만족스러워요!")
								.rating(5 - (int)(i % 2)) // 별점 5점, 4점 번갈아가며 부여
								.imageUrl("https://www.google.com/images/branding/googlelogo/2x/googlelogo_color_272x92dp.png")
								.build();
						
						reviewRepository.save(review);
					}
					System.out.println("테스트용 리뷰 데이터 5개 생성 완료 (유저 ID 1~5 사용)");
				}
				
			// 3. 메뉴 테스트 데이터 생성
				if (menuRepository.count() == 0) {
					Menu menu1 = Menu.builder().name("아이스아메리카노").price(6000L).stockQuantity(10).build();
					Menu menu2 = Menu.builder().name("아이스카페라떼").price(7000L).stockQuantity(10).build();
					Menu menu3 = Menu.builder().name("민트초코프라푸치노").price(15000L).stockQuantity(10).build();
					Menu menu4 = Menu.builder().name("아이스바닐라라떼").price(5000L).stockQuantity(10).build();
					Menu menu5 = Menu.builder().name("에스프레소").price(7500L).stockQuantity(10).build();
					
					// Repository에 일괄 저장
					menuRepository.save(menu1);
					menuRepository.save(menu2);
					menuRepository.save(menu3);
					menuRepository.save(menu4);
					menuRepository.save(menu5);
					
					System.out.println("테스트용 메뉴 데이터 5개 생성 완료");
				}
	}
	
	
    
    
    
}
