package com.ssafy.pickpay.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ssafy.pickpay.domain.Order;


public interface OrderRepository extends JpaRepository<Order, Long>{

}
