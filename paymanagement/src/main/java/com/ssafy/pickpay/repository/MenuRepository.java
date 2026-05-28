package com.ssafy.pickpay.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ssafy.pickpay.domain.Menu;

public interface MenuRepository extends JpaRepository<Menu, Long> {

}
