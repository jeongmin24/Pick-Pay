package com.miniproject.server.config;

import com.miniproject.server.domain.jwt.service.JwtService;
import com.miniproject.server.domain.user.entity.UserRoleType;
import com.miniproject.server.filter.JWTFilter;
import com.miniproject.server.filter.LoginFilter;
import com.miniproject.server.handler.RefreshTokenLogoutHandler;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;

import java.util.List;

/**
 * 암호화된 비밀번호 저장을 위한 passwordEncoder -> SpringSecurity가 제공하는걸 사용하자
 * */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final AuthenticationConfiguration authenticationConfiguration;
    private final AuthenticationSuccessHandler loginSuccessHandler;
    private final AuthenticationSuccessHandler socialSuccessHandler;
    private final JwtService jwtService;


    public SecurityConfig(
            AuthenticationConfiguration authenticationConfiguration,
            @Qualifier("LoginSuccessHandler") AuthenticationSuccessHandler loginSuccessHandler, // @Qualifier 선언
            @Qualifier("SocialSuccessHandler") AuthenticationSuccessHandler socialSuccessHandler, // @Qualifier 선언
            JwtService jwtService)
    {
        this.authenticationConfiguration = authenticationConfiguration;
        this.loginSuccessHandler = loginSuccessHandler;
        this.socialSuccessHandler = socialSuccessHandler;
        this.jwtService = jwtService;
    }

    // 비밀번호 단방향(Bcrypt) 암호화용 Bean
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 권한 계층
    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withRolePrefix("ROLE_")
                .role(UserRoleType.ADMIN.name()).implies(UserRoleType.USER.name())
                .build();
    }

    // CORS Bean
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Authorization", "Set-Cookie"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // 커스텀 자체 로그인 필터를 위한 AuthenticationManager Bean 수동 등록
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    // SecurityFilterChain Bean 등록
    // 기본적인 시큐리티필터체인을 위한 설정
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        /**
         * CSRF 보안 필터 disable
         * */
         http
                .csrf(AbstractHttpConfigurer::disable);

         /**
          * CORS 설정
          * */
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()));

         /**
         * 기본 Form 기반 인증 필터들 disable
         * */

         //멀티팟드(서버 여러개) 환경에서는 기존 폼로그인(세션로그인) 방식이 안맞음
         //-> JSON 요청 + JWT 기반 로그인 필터로 전환
        // Spring Security가 제공하는 기본 로그인 필터(UsernamePasswordAuthenticationFilter)를 비활성화(disable)하겠다
        http
                .formLogin(AbstractHttpConfigurer::disable);

        /**
         * 기본 Basic 인증 필터 disable
         */
        // http 기반의 로그인 X
        http
                .httpBasic(AbstractHttpConfigurer::disable);

        /**
         * 인가 : 권한 확인
         * JWT 안의 권한 정보 설정
         * */
        // 사용자 컨트롤러의 api에 대해서 접근 허용 / 거부 / 로그인해야 접근 가능한지 설정
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/jwt/exchange", "/jwt/refresh").permitAll()
                        .requestMatchers(HttpMethod.POST, "/user/exist", "/user").permitAll()
                        .requestMatchers(HttpMethod.GET, "/user").hasRole(UserRoleType.USER.name()) // 권한 계층 설정을 통해 USER보다 권한이 높은 ADMIN은 자동으로 접근 가능
                        .requestMatchers(HttpMethod.PUT, "/user").hasRole(UserRoleType.USER.name())
                        .requestMatchers(HttpMethod.DELETE, "/user").hasRole(UserRoleType.USER.name())
                        .anyRequest().authenticated() // 외의 경로는 인증된 사용자여야 접근할 수 있도록
                );

        /**
         * 예외처리
         * */
        http
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED); // 401 응답 (로그인 하지 않은 상태)
                        })
                        .accessDeniedHandler((request, response, authException) -> {
                            response.sendError(HttpServletResponse.SC_FORBIDDEN); // 403 응답 (로그인 했지만 권한 없음)
                        })
                );

        /**
         * 커스텀 필터추가 - 직접 작성한 LoginFilter를 SecurityFilterChain에 등록 + 성공핸들러 등록
         * */
        http
                .addFilterBefore(new LoginFilter(authenticationManager(authenticationConfiguration), loginSuccessHandler), UsernamePasswordAuthenticationFilter.class);
        // 세큐리티필터체인 중 특정 필터 앞에 추가
        // 이때 생성되는 LoginFilter는 인증을 담당하는 authenticationManager를 주입 받는다

        /**
         * 커스텀 필터 - JWTFilter추가
         * 로그아웃 필터보다 앞에 등록되도록 세팅
         * */
        http
                .addFilterBefore(new JWTFilter(), LogoutFilter.class);

        /**
         * 세션 필터 설정(STATELESS)
         * */
        http
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        /**
         *OAuth2 인증용, oauth2 변수에 대해서 활성화
         * successHandler는 로그인 성공시 jwt 발급을 위한 커스텀 핸들러
         * */
        http
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(socialSuccessHandler));

        /**
         *  기본 로그아웃 필터 + 커스텀 리프레시 토큰 삭제 핸들러 추가
         * */
        http
                .logout(logout -> logout
                        .addLogoutHandler(new RefreshTokenLogoutHandler(jwtService)));





        return http.build();
    }
}
