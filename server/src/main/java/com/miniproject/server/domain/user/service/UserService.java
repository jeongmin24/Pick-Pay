package com.miniproject.server.domain.user.service;

import com.miniproject.server.domain.jwt.service.JwtService;
import com.miniproject.server.domain.user.dto.CustomOAuth2User;
import com.miniproject.server.domain.user.dto.UserRequestDto;
import com.miniproject.server.domain.user.dto.UserResponseDto;
import com.miniproject.server.domain.user.entity.SocialProviderType;
import com.miniproject.server.domain.user.entity.UserEntity;
import com.miniproject.server.domain.user.entity.UserRoleType;
import com.miniproject.server.domain.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.security.Security;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class UserService extends DefaultOAuth2UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(PasswordEncoder passwordEncoder, UserRepository userRepository, JwtService jwtService) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository; // UserService에서 UserRepository 사용
        this.jwtService = jwtService; // 회원 탈퇴 시 회원의 리프레시 토큰을 제거해야함
    }

    // 1. 자체 로그인 회원가입 (존재 여부)
    // 프론트에서 중복확인을 눌렀을때 백엔드에서 API로 사용
    @Transactional(readOnly = true)
    public Boolean existUser(UserRequestDto dto) {
        return userRepository.existsByUsername(dto.getUsername());
    }

    // 2. 자체 로그인 회원가입
    // 생성된 User의 id를 리턴
    @Transactional
    public Long addUser(UserRequestDto dto) {

        // 사용자로부터 받은 dto에서 username(사용자 id)이 DB에 있는지 검증
        // 프론트로부터가 아니라 postman, 해킹툴을 이용해서 백엔드에 직접 요청을 보낼경우 방지
        if(userRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("이미 유저가 존재합니다.");
        }

        // 사용자가 DB에 없는 경우 dto로부터 DB에 저장
        // builder 형식으로 객체 생성
        UserEntity entity = UserEntity.builder()
                .username(dto.getUsername())
                .password(passwordEncoder.encode(dto.getPassword())) // 회원가입할때 password는 인코딩
                .isLock(false)
                .isSocial(false)
                .roleType(UserRoleType.USER) // 우선 일반 유저로 가입
                .nickname(dto.getNickname())
                .email(dto.getEmail())
                .build();

        // 유터리포지토리의 save쿼리를 통해 엔티티를 저장하고 저장한 엔티티에서 id를 받아 반환
        return userRepository.save(entity).getId();
    }

    // 3. 자체 로그인
    // loadByUsername() : JWT로 인증받는 사용자를 스프링시큐리티가 인식할 수 있는 형태로 만들어줌
    // JWT에 있는 username으로 임시 사용자 객체(UserDetails, 요청을 처리하는 용도의 객체)를 만드는 역할
    @Transactional(readOnly = true)
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        UserEntity entity = userRepository.findByUsernameAndIsLockAndIsSocial(username, false, false)
                .orElseThrow(()-> new UsernameNotFoundException(username));

        // 조회한 entity를 바탕으로 UserDetails를 만들어서 리턴
        // Authentication에 저장할 인증객체 UserDetails 구현체(User)로 빌드하여 생성
        return User.builder()
                .username(entity.getUsername())
                .password(entity.getPassword())
                .roles(entity.getRoleType().name())
                .accountLocked(entity.getIsLock())
                .build();
    }

    // 4. 자체 로그인 회원 정보 수정
    // 회원 정보 수정시 자체로그인 여부, 잠김 여부를 확인해야함
    // 검증하지 않을 경우 소셜로그인 정보를 변경할 위험
    @Transactional
    public Long updateUser(UserRequestDto dto) throws AccessDeniedException {

        // 본인만 수정 가능 검증
        String sessionUsername = SecurityContextHolder.getContext().getAuthentication().getName(); // 현재 스레드의 로그인된 사용자의 username값을 받아옴
        if(!sessionUsername.equals(dto.getUsername())) { // 받아온 username이 dto의 username과 동일한 경우에만 수정 가능
            throw new AccessDeniedException("본인 계정만 수정 가능");
        }

        // 조회
        UserEntity entity = userRepository.findByUsernameAndIsLockAndIsSocial(dto.getUsername(), false, false) // 잠겨있지않고 소셜로그인이 아닌 계정만 엔티티 생성
                .orElseThrow(()-> new UsernameNotFoundException(dto.getUsername()));

        // 회원 정보 수정
        entity.updateUser(dto);

        return userRepository.save(entity).getId();
    }


    // 5. 소셜 로그인 (매 로그인시: 신규=가입, 기존=업데이트)
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        // 부모 기본 로직 -> accessToken으로 사용자 정보(JSON) 받아오기
        OAuth2User oAuth2User= super.loadUser(userRequest);

        /**
         * 우리 서비스 규칙에 맞게 커스터마이징
         * UserEntity에 맞게, provider 구분 등
         * */
        // 데이터 파싱
        Map<String, Object> attributes;
        List<GrantedAuthority> authorities;

        String username;
        String role = UserRoleType.USER.name();
        String email;
        String nickname;

        // provider 구분 (네이버, 구글)
        String registrationId = userRequest.getClientRegistration().getRegistrationId().toUpperCase();
        if (registrationId.equals(SocialProviderType.NAVER.name())) {

            attributes = (Map<String, Object>) oAuth2User.getAttributes().get("response");
            username = registrationId + "_" + attributes.get("id");
            email = attributes.get("email").toString();
            nickname = attributes.get("nickname").toString();

        } else if (registrationId.equals(SocialProviderType.GOOGLE.name())) {

            attributes = (Map<String, Object>) oAuth2User.getAttributes();
            username = registrationId + "_" + attributes.get("sub");
            email = attributes.get("email").toString();
            nickname = attributes.get("name").toString();

        } else {
            throw new OAuth2AuthenticationException("지원하지 않는 소셜 로그인입니다.");
        }

        // DB 조회 ( 기존 유저인지 확인 )
        Optional<UserEntity> entity = userRepository.findByUsernameAndIsSocial(username, true);
        if(entity.isPresent()) {

            // 기존 유저의 role 가져오기
            role = entity.get().getRoleType().name();

            // 기존 유저 닉네임 / 이메일 갱신
            UserRequestDto dto =  new UserRequestDto();
            dto.setNickname(nickname);
            dto.setEmail(email);
            entity.get().updateUser(dto);
            userRepository.save(entity.get());

        } else {
            // 신규 유저 등록
            UserEntity newUserEntity = UserEntity.builder()
                    .username(username)
                    .password("") // 소셜로그인은 패스워드 X
                    .isLock(false)
                    .isSocial(true)
                    .socialProviderType(SocialProviderType.valueOf(registrationId))
                    .roleType(UserRoleType.USER)
                    .nickname(nickname)
                    .email(email)
                    .build();

            userRepository.save(newUserEntity);
        }

        // 권한 리스트 생성
        // 로그인 후 인증 컨텍스트에 권한 (ROLE_USER, ROLE_ADMIN)을 전달
        authorities = List.of(new SimpleGrantedAuthority(role));

        // 커스텀 OAuth2User 반환
        // 로그인한 유저 객체를 Authentication이 저장해야되는데
        // 식별자(username), 소셜 계정 데이터(attributes), 권한(authorities) 필수
        return new CustomOAuth2User(attributes, authorities, username);


    }

    // 6. 자체/소셜 유저 정보 조회
    @Transactional(readOnly = true)
    public UserResponseDto readUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName(); // 현재 세션에 있는 username 조회

        UserEntity entity = userRepository.findByUsernameAndIsLock(username, false)
                .orElseThrow(() -> new UsernameNotFoundException("해당 유저를 찾을 수 없습니다: " + username));

        return new UserResponseDto(username, entity.getIsSocial(), entity.getNickname(), entity.getEmail());
    }

    // 7. 자체/소셜 로그인 회원 탈퇴
    @Transactional
    public void deleteUser(UserRequestDto dto) throws AccessDeniedException {

        // 본인 및 어드민만 삭제 가능 검증
        SecurityContext context = SecurityContextHolder.getContext();
        String sessionUsername = context.getAuthentication().getName();
        String sessionRole = context.getAuthentication().getAuthorities().iterator().next().getAuthority();

        boolean isOwner = sessionUsername.equals(dto.getUsername());
        boolean isAdmin = sessionRole.equals("ROLE_"+UserRoleType.ADMIN.name());

        if(!isOwner&&!isAdmin) {
            throw new AccessDeniedException("본인 혹은 관리자만 삭제할 수 있습니다");
        }

        //유저 제거
        userRepository.deleteByUsername(dto.getUsername());

        //리프레시 토큰 제거
        jwtService.removeRefreshUser(dto.getUsername());
    }

}
