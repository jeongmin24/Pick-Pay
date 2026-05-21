package com.ssafy.pickpay.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ssafy.pickpay.domain.User;
import java.util.List;


public interface UserRepository extends JpaRepository<User, Long>{
	
	Boolean existsByLoginId(String loginId); // 회원가입 시 로그인아이디가 존재하는지 중복 검증
	Optional<User> findByLoginId(String loginId); // 회원정보 수정시 자체 로그인 여부 확인 
	void deleteByLoginId(String loginId);

}
