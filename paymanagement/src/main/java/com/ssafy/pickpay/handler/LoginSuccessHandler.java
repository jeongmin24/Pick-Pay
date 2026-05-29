package com.ssafy.pickpay.handler;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.ssafy.pickpay.domain.CustomUserDetails;
import com.ssafy.pickpay.service.CustomUserDetailsService;
import com.ssafy.pickpay.service.JwtService;
import com.ssafy.pickpay.util.JWTUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Qualifier("LoginSuccessHandler")
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final CustomUserDetailsService customUserDetailsService;

	private final JwtService jwtService;

	public LoginSuccessHandler(JwtService jwtService, CustomUserDetailsService customUserDetailsService) {
		super();
		this.jwtService = jwtService;
		this.customUserDetailsService = customUserDetailsService;
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {

		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		
		Long userId = userDetails.getUserId();
		String loginId = userDetails.getLoginId();
		String role = userDetails.getAuthorities().iterator().next().getAuthority(); // ROLE_USER

		String accessToken = JWTUtil.createJWT(userId, loginId, role, true);
		String refreshToken = JWTUtil.createJWT(userId, loginId, role, false);

		// 발급한 리프레시 DB 테이블 저장
		jwtService.addRefresh(loginId, refreshToken);

		response.setContentType("application/json"); // json
		response.setCharacterEncoding("UTF-8"); // 인코딩

		String json = String.format("{\"accessToken\":\"%s\", \"refreshToken\":\"%s\"}", accessToken, refreshToken);
		response.getWriter().write(json);
		response.getWriter().flush();

	}

}
