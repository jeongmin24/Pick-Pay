package com.miniproject.server.handler;

import com.miniproject.server.domain.jwt.service.JwtService;
import com.miniproject.server.util.JWTUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
/**
 * 로그인 성공 시 프론트로 JWT 발급 핸들러
 *
 * 자체로그인, 소셜로그인 각각 성공핸들러에서 jwt 발급할것
 * */

@Component
@Qualifier("LoginSuccessHandler") // 자체로그인과 소셜로그인의 성공 핸들러가 같을 경우 빈이 중복될 수 있음
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;

    public LoginSuccessHandler(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    // 로그인 성공후 오는 것이므로 로그인 성공 객체 authentication을 인자로줌
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        // username, role
        String username = authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        // JWT (액세스/리프레시) 발급
        String accessToken = JWTUtil.createJWT(username, role, true);
        String refreshToken = JWTUtil.createJWT(username, role, false);

        // 발급한 리프레시 DB 테이블 저장 ( 리프레시 whitelist )
        jwtService.addRefresh(username, refreshToken);

        // 응답
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String json = String.format("{\"accessToken\":\"%s\", \"refreshToken\":\"%s\"}", accessToken, refreshToken);
        response.getWriter().write(json);
        response.getWriter().flush();

    }
}
