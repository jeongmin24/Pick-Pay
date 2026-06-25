package com.ssafy.pickpay.config;

import org.springframework.beans.factory.annotation.Value;
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
	
	@Value("${server.port:8080}")
    private String port;
	
	@Override
	@Transactional
	public void run(String... args) throws Exception {
		if(userRepository.count() == 0) {
			String localIp = java.net.InetAddress.getLocalHost().getHostAddress();
		    String baseUrl = "http://" + localIp + ":" + port;
			
			User ssafyUser = User.builder()
		            .loginId("ssafy15")
		            .password(passwordEncoder.encode("1234")) // 비밀번호는 편의상 1234로 설정했습니다.
		            .nickname("김싸피")
		            .imageUrl(baseUrl + "/images/profileImgB.png")
		            .build();
		    userRepository.save(ssafyUser);

		    User choiUser = User.builder()
		            .loginId("chlqhrud0208")
		            .password(passwordEncoder.encode("1234"))
		            .nickname("최두근")
		            .imageUrl(baseUrl + "/images/profileImgG.png")
		            .build();
		    userRepository.save(choiUser);
		    
		    User leeUser = User.builder()
		            .loginId("jeongmin24")
		            .password(passwordEncoder.encode("1234"))
		            .nickname("얼렁뚱땡이")
		            .imageUrl(baseUrl + "/images/profileImgL.png")
		            .build();
		    userRepository.save(leeUser);
		    
		    User OtherUser1 = User.builder()
		            .loginId("otherUser1")
		            .password(passwordEncoder.encode("1234")) // 비밀번호는 편의상 1234로 설정했습니다.
		            .nickname("벼락부자핫도그")
		            .imageUrl(baseUrl + "/images/profileImg.png")
		            .build();
		    userRepository.save(OtherUser1);
		    
		    User OtherUser2 = User.builder()
		            .loginId("otherUser2")
		            .password(passwordEncoder.encode("1234")) // 비밀번호는 편의상 1234로 설정했습니다.
		            .nickname("요즘잘자쿨냥이")
		            .imageUrl(baseUrl + "/images/profileImg.png")
		            .build();
		    userRepository.save(OtherUser2);
		    
		    for (int i = 1; i <= 10; i++) {
                User user = User.builder()
                        .loginId("user" + i)
                        .password(passwordEncoder.encode("1234")) 
                        .nickname("user" + i)
                        .imageUrl(baseUrl + "/images/profileImg.png")
                        .build();
                userRepository.save(user);
            }
			System.out.println("테스트용 유저 데이터 10개 생성 완료");
		}
		
		// 2. 리뷰 테스트 데이터 생성 (유저 ID 1~5번을 그대로 활용 ⭕)
		if (reviewRepository.count() == 0) {
		    // 1. 현재 이 서버가 구동 중인 PC의 로컬 IP 주소와 포트를 동적으로 가져옵니다.
		    String localIp = java.net.InetAddress.getLocalHost().getHostAddress();
		    String baseUrl = "http://" + localIp + ":" + port;

		    // 메뉴 이름이 들어가지 않은 자연스러운 더미 리뷰 텍스트 배열
		    String[] contents = {
		        "음료 양도 많고 상큼해서 여름에 마시기 딱 좋아요! 토핑도 신선하고 만족스럽습니다. 매일 주문하고 싶을 정도예요.",
		        "여기 베이커리 맛집이네요. 겉은 바삭하고 속은 쫄깃하면서 버터 향이 진하게 나서 아주 맛있습니다. 시원한 커피랑 조합이 정말 좋아요.",
		        "디저트가 정말 꾸덕하고 찐해요! 단것 당길 때 최고입니다. 달달한 맛 덕분에 스트레스가 확 풀리네요. 강력 추천합니다!",
		        "커피 주문했는데 원두 향이 깊고 에스프레소 비율이 딱 적당해서 너무 맛있게 마셨습니다. 얼음이 녹아도 밍밍하지 않고 진하네요.",
		        "빵이 정말 부드럽고 입에서 살살 녹아요. 안에 들어있는 크림도 전혀 느끼하지 않고 고소해서 누구나 좋아할 맛입니다. 디저트 퀄리티가 전반적으로 훌륭해요."
		    };

		    for (long i = 1; i <= 5; i++) {
		        User writer = userRepository.findById(i)
		                .orElseThrow(() -> new IllegalStateException("테스트 유저를 찾을 수 없습니다."));

		        Review review = Review.builder()
		                .user(writer) 
		                .content(contents[(int)(i - 1)]) // 0번부터 4번까지 내용 매핑
		                .rating(5 - (int)(i % 2)) 
		                // 💡 핵심: review1.png ~ review5.png 이름을 그대로 규칙에 맞게 생성
		                .imageUrl(baseUrl + "/images/review" + i + ".png") 
		                .build();
		        
		        reviewRepository.save(review);
		    }
		    System.out.println("테스트용 리뷰 데이터 5개 생성 완료 (유저 ID 1~5 사용, review1~5.png 적용)");
		}
				
			// 3. 메뉴 테스트 데이터 생성
				if (menuRepository.count() == 0) {
					Menu menu1 = Menu.builder().name("블루베리스무디").price(6000L).stockQuantity(10).imageUrl("blueberry.png").build();
					Menu menu2 = Menu.builder().name("에스프레소").price(7000L).stockQuantity(10).imageUrl("coffee.png").build();
					Menu menu3 = Menu.builder().name("초코프라푸치노").price(15000L).stockQuantity(10).imageUrl("cookiecream.png").build();
					Menu menu4 = Menu.builder().name("아이스아메리카노").price(5000L).stockQuantity(10).imageUrl("icecoffee.png").build();
					Menu menu5 = Menu.builder().name("망고스무디").price(7500L).stockQuantity(10).imageUrl("icefine.png").build();
					Menu menu6 = Menu.builder().name("레몬에이드").price(7500L).stockQuantity(10).imageUrl("lemonade.png").build();
					
					// Repository에 일괄 저장
					menuRepository.save(menu1);
					menuRepository.save(menu2);
					menuRepository.save(menu3);
					menuRepository.save(menu4);
					menuRepository.save(menu5);
					menuRepository.save(menu6);
					
					System.out.println("테스트용 메뉴 데이터 5개 생성 완료");
				}
	}
	
	
    
    
    
}
