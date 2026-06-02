package com.ssafy.pickpay.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ssafy.pickpay.domain.OrderItems;

public interface OrderItemsRepository extends JpaRepository<OrderItems, Long>{

	List<OrderItems> findByOrder_OrderId(Long orderId); // order_id가 n번인 OrderItems 전부 리스트로 찾아오기
}
