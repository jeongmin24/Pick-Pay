package com.ssafy.pickpay;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.google.firebase.FirebaseApp;

@SpringBootTest
public class FirebaseConfigTest {
	
	@Test
	@DisplayName("애플리케이션 시작시 FirebaseApp 인스턴스가 성공적으로 초기화되어 활성화된다")
	void firebaseInitializationTest() {
		int initializedAppsCount = FirebaseApp.getApps().size();
		
		assertThat(initializedAppsCount).isGreaterThan(0); // 활성화된 앱 개수가 최소 1개 이상이어야함
		
		assertThat(FirebaseApp.getInstance()).isNotNull();
	}

}
