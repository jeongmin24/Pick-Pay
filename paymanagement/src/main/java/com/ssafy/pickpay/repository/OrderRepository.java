package com.ssafy.pickpay.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ssafy.pickpay.domain.Order;


public interface OrderRepository extends JpaRepository<Order, Long>{
	List<Order> findByGroupOrder_GroupId(Long groupId);
}
