package com.ssafy.pickpay.filter;

import java.io.IOException;
import java.net.Authenticator;
import java.util.Collections;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.ssafy.pickpay.util.JWTUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JWTFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		
		String authorization = request.getHeader("Authorization");
		
		if( authorization == null || !authorization.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}
		
		
		String accessToken = authorization.split(" ")[1];
		
		if(JWTUtil.isValid(accessToken, true)) {
			String loginId = JWTUtil.getLoginId(accessToken);
			String role = JWTUtil.getRole(accessToken);
			
			List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(role)); // 권한리스트
			
			Authentication auth = new UsernamePasswordAuthenticationToken(loginId, null, authorities);
			SecurityContextHolder.getContext().setAuthentication(auth);
			
			filterChain.doFilter(request, response);
		} else {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"토큰 만료 또는 유효하지 않은 토큰\"}");
            return;
		}
	} 

}
