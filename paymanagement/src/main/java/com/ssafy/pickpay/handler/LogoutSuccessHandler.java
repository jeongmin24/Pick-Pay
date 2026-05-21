package com.ssafy.pickpay.handler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.management.RuntimeErrorException;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.util.StringUtils;

import com.ssafy.pickpay.service.JwtService;
import com.ssafy.pickpay.util.JWTUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public class LogoutSuccessHandler implements LogoutHandler {

	private final JwtService jwtService;
	
	
	public LogoutSuccessHandler(JwtService jwtService) {
		super();
		this.jwtService = jwtService;
	}


	@Override
	public void logout(HttpServletRequest request, HttpServletResponse response,
			@Nullable Authentication authentication) {
		
		try {
			
			String body = new BufferedReader(new InputStreamReader(request.getInputStream()))
	                .lines().reduce("", String::concat);

	        if (!StringUtils.hasText(body)) return;

	        ObjectMapper mapper = new ObjectMapper();
	        JsonNode jsonNode = mapper.readTree(body);
	        String refreshToken = jsonNode.has("refreshToken") ? jsonNode.get("refreshToken").asText() : null;

	        // 유효성 검증
	        if (refreshToken == null) {
	            return;
	        }
	        Boolean isValid = JWTUtil.isValid(refreshToken, false);
	        if (!isValid) {
	            return;
	        }

	        // Refresh 토큰 삭제
	        jwtService.removeRefresh(refreshToken);
	        
		} catch(IOException e) {
			throw new RuntimeException("리프레시 토큰을 읽는데 실패했습니다", e);
		}
		
		
	}

}
