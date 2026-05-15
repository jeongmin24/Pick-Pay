package com.miniproject.server.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;

// 순수 자바 클래스
// jwt 생성/검증 서비스에서 사용
public class JWTUtil {

    private static final SecretKey secretKey; // 시크릿 키
    private static final Long accessTokenExpiresIn; // 액세스토큰 생명주기
    private static final Long refreshTokenExpiresIn; // 리프레시토큰 생명주기

    // static 영역 생성자
    // static 클래스가 생성될때 작동
    static {
        // 시크릿키 문자열을 만들고 그 문자열로 시크릿키 객체 생성
        String secretKeyString = "himynameiskimjihunmyyoutubechann";
        secretKey = new SecretKeySpec(secretKeyString.getBytes(StandardCharsets.UTF_8), Jwts.SIG.HS256.key().build().getAlgorithm());

        accessTokenExpiresIn = 3600L * 1000; // 1시간
        refreshTokenExpiresIn = 604800L * 1000; // 7일

    }

    /**
     * 토큰을 검증하고나면 바디에 있는 데이터 파싱
     * */
    // JWT 클레임 (바디 내부) username 파싱 (jwt 바디의 username을 가져오는 함수)
    public static String getUsername(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("sub", String.class);
    }

    // JWT 클레임 role 파싱
    public static String getRole(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("role", String.class);
    }

    // JWT 유효 여부 (위조, 시간, Access/Refresh 여부) 검증 -> 우리가 만든 JWT인지, 유효시간은 끝났는지, 위조되었는지 체크
    // 유효하다면 true, 아니라면 false리턴
    public static Boolean isValid(String token, Boolean isAccess) {
        // 유효시간이 지나면 exception으로 감
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String type = claims.get("type", String.class);
            if (type == null) return false;

            if (isAccess && !type.equals("access")) return false;
            if (!isAccess && !type.equals("refresh")) return false;

            return true;

        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }


    // JWT(Access/Refresh) 생성
    public static String createJWT(String username, String role, Boolean isAccess) {

        // 현재 시간 기준으로
        // access인지 , refresh인지에 따라 ExpiresIn 만큼을 더해서 현재시간 + ExpiresIn = "생명시간" 부여
        long now = System.currentTimeMillis();
        long expiry = isAccess ? accessTokenExpiresIn : refreshTokenExpiresIn; // isAccess가 true면 accessToken생성 false면 refreshToken생성
        String type = isAccess ? "access" : "refresh";

        return Jwts.builder()
                .claim("sub", username) // (payload)
                .claim("role", role) // (payload)
                .claim("type", type) // access / refresh (payload)
                .issuedAt(new Date(now)) // 발급시간
                .expiration(new Date(now + expiry))
                .signWith(secretKey)
                .compact();
    }
}
