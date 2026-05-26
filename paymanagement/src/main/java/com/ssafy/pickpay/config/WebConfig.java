package com.ssafy.pickpay.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// CORS 설정은 추후 웹뷰나 웹관리자 페이지 도입시 적용 
//@Configuration
//public class WebConfig implements WebMvcConfigurer {
//	
//	@Override
//	public void addCorsMappings(CorsRegistry registry) {
//		WebMvcConfigurer.super.addCorsMappings(registry);
//		registry.addMapping("/**").allowedOrigins("*").allowedMethods("*");
//	}
//
//}
