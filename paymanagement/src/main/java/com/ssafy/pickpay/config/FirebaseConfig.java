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
                    .setDatabaseUrl("https://Pickpay.firebaseio.com") // 콘솔에서 확인 가능
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

}
