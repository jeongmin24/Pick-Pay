package com.ssafy.pickpay.config;

import com.ssafy.pickpay.handler.LoginSuccessHandler;
import com.ssafy.pickpay.handler.LogoutSuccessHandler;
import com.ssafy.pickpay.service.JwtService;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;

import com.ssafy.pickpay.filter.JWTFilter;
import com.ssafy.pickpay.filter.LoginFilter;

import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
	
	private final LoginSuccessHandler loginSuccessHandler_1;
	private final AuthenticationConfiguration authenticationConfiguration;
	private final AuthenticationSuccessHandler loginSuccessHandler;
	private final JwtService jwtService;
	private final ObjectMapper objectMapper;
	
	public SecurityConfig(AuthenticationConfiguration authenticationConfiguration, @Qualifier("LoginSuccessHandler") AuthenticationSuccessHandler loginSuccessHandler, LoginSuccessHandler loginSuccessHandler_1, JwtService jwtService, ObjectMapper objectMapper) {
		super();
		this.authenticationConfiguration = authenticationConfiguration;
		this.loginSuccessHandler = loginSuccessHandler;
		this.loginSuccessHandler_1 = loginSuccessHandler_1;
		this.jwtService = jwtService;
		this.objectMapper = objectMapper;
	}
	

	// 비밀번호 단방향 암호화용 빈 
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	
	// 로그인 필터 AuthenticationManager 
	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
	    return configuration.getAuthenticationManager();
	}
	
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

	    // CSRF, 기본 폼로그인, Basic 인증 비활성화
	    http
	            .csrf(AbstractHttpConfigurer::disable);

	    http
	            .formLogin(AbstractHttpConfigurer::disable);

	    http
	            .httpBasic(AbstractHttpConfigurer::disable);

	    // 인가
	    http
	            .authorizeHttpRequests(auth -> auth
	            		.requestMatchers("/jwt/exchange", "/jwt/refresh").permitAll()
	            		.requestMatchers(HttpMethod.POST, "/user/exist", "/user").permitAll()
	            		.requestMatchers("/error").permitAll()
	            		.requestMatchers(HttpMethod.GET, "/user").hasRole("USER")
	            		.requestMatchers(HttpMethod.PUT, "/user").hasRole("USER")
	            		.requestMatchers(HttpMethod.DELETE, "/user").hasRole("USER")
	                    .anyRequest().authenticated());

	    // 예외 처리
	    http
	            .exceptionHandling(e -> e
	                    .authenticationEntryPoint((request, response, authException) -> {
	                    	response.setContentType("application/json;charset=UTF-8");
	                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 응답
	                        Map<String, String> errorResponse = new HashMap<>();
	                        errorResponse.put("message", "인증 정보가 유효하지 않습니다");
	                        String jsonResult = objectMapper.writeValueAsString(errorResponse);
	                        response.getWriter().write(jsonResult);
	                        
	                    })
	                    // 403 응답은 RestControllerAdvice에서 
	            );
	    // 커스텀 필터 등록
	    http.addFilterBefore(new LoginFilter(authenticationManager(authenticationConfiguration), loginSuccessHandler), UsernamePasswordAuthenticationFilter.class);

	    // 세션 필터 설정 (STATELESS)
	    http
	            .sessionManagement(session -> session
	                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS));
	    // 로그아웃 핸들러 등록 
	    http
	    	.logout(logout -> logout.addLogoutHandler(new LogoutSuccessHandler(jwtService)));
	    http
	    	.addFilterBefore(new JWTFilter(), LogoutFilter.class);
	    

	    return http.build();
	}

}
