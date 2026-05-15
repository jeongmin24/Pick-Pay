package com.miniproject.server.filter;

import com.miniproject.server.util.JWTUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
/**
 * 헤더에 오는 JWT(액세스)를 검증하여 스레드에 배정되는 시큐리티컨텍스트에 username과 role부여 -> Authentication 생성
 * JWTAuthenticationFilter의 커스텀 버전
 * */
public class JWTFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String authorization = request.getHeader("Authorization");

        // Authorization이 없거나 유효하지 않은 경우
        if (authorization == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!authorization.startsWith("Bearer ")) {
            throw new ServletException("Invalid JWT token");
        }

        // 토큰 파싱
        String accessToken = authorization.split(" ")[1];

        // JWT 유효성 검사
        if (JWTUtil.isValid(accessToken, true)) {

            // username, role값 파싱, jwt payload 안의 클레임 복원
            String username = JWTUtil.getUsername(accessToken);
            String role = JWTUtil.getRole(accessToken);

            // 하나의 시큐리티 컨텍스트 생성 및 Authentication 객체 재생성
            List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(role));

            Authentication auth = new UsernamePasswordAuthenticationToken(username, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);

            // 다음 필터로 전달
            filterChain.doFilter(request, response);

        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"토큰 만료 또는 유효하지 않은 토큰\"}");
            return;
        }

    }

}