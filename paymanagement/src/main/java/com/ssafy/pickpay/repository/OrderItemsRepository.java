package com.ssafy.pickpay.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ssafy.pickpay.domain.OrderItems;

public interface OrderItemsRepository extends JpaRepository<OrderItems, Long>{

}
