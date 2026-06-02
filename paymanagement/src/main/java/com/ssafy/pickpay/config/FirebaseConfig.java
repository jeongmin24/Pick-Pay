package com.ssafy.pickpay.config;

import java.io.InputStream;

import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import jakarta.annotation.PostConstruct;

@Configuration
public class FirebaseConfig {
	
	@PostConstruct
	public void init() {
		try {
			InputStream serviceAccount = getClass().getResourceAsStream("/firebase-service-key.json");

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl("https://pickpay-be337-default-rtdb.firebaseio.com") // 콘솔에서 확인 가능
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                System.out.println("Firebase Admin SDK 초기화 성공");
            } else {
            	System.out.println("Firebase App이 이미 초기화 되어있습니다.");
            }
            
            System.out.println("활성화된 Firebase 앱 개수:" + FirebaseApp.getApps().size());
		} catch(Exception e) {
			System.out.println("firebase 초기화 중 에러 발생");
			e.printStackTrace();
			throw new IllegalStateException("Firebase 초기화 실패", e);
		}
	}

}
