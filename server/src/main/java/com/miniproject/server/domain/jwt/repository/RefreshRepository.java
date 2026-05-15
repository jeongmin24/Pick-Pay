package com.miniproject.server.domain.jwt.repository;

import com.miniproject.server.domain.jwt.entity.RefreshEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * JpaRepository<엔티티 타입, 엔티티의 PK 타입>
 * RefreshEntity 테이블을 대상으로 id가 Long 타입인 데이터를 CRUD할 수 있는 리포지토리를 만들겠다
 * */
@Repository
public interface RefreshRepository extends JpaRepository<RefreshEntity, Long> {

    Boolean existsByRefresh(String refreshToken);
    @Transactional // 삭제의 경우 선언해야됨
    void deleteByRefresh(String refreshToken);
    @Transactional
    void deleteByUsername(String username);

    void deleteByCreatedDateBefore(LocalDateTime createdDate);
}
