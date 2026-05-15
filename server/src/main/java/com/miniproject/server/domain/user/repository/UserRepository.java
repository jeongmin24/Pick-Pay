package com.miniproject.server.domain.user.repository;

import com.miniproject.server.domain.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * repository: DB에 접근해서 CRUD를 날리는 역할
 * */
public interface UserRepository extends JpaRepository<UserEntity, Long> { // UserEntity와 UserEntity의 id타입인 Long 상속
    Boolean existsByUsername(String username);

    //쿼리를 통해 회원 정보 수정시 자체 로그인 여부, 잠김 여부 확인
    /**
     * SELECT * FROM user WHERE username = :username AND is_lock = :isLock AND is_social = :isSocial
     * */
    Optional<UserEntity> findByUsernameAndIsLockAndIsSocial(String username, Boolean isLock, Boolean isSocial);

    Optional<UserEntity> findByUsernameAndIsSocial (String username, Boolean social);

    @Transactional
    void deleteByUsername(String username);

    Optional<UserEntity> findByUsernameAndIsLock(String username, Boolean isLock);
}
