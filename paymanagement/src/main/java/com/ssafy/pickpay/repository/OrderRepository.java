package com.ssafy.pickpay.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ssafy.pickpay.domain.Order;


public interface OrderRepository extends JpaRepository<Order, Long>{
	List<Order> findByGroupOrder_GroupId(Long groupId);
	Optional<Order> findByOrderIdAndUser_UserIdAndGroupOrderIsNull(Long orderId, Long userId); //groupOrderIsNull조건 -> 개별 주문 영수증 
	// findBy : SELECT * FROM Order WHERE order_id = ? AND user_id = ? AND group_order_id IS NULL
	// Q : SELECT o.* FROM orders o JOIN users u o.user_id = u.user_id WHERE o.order_id = ? AND u.user_id = ? AND o.group_order_id IS NULL;
	// User_UserId: _ = 객체 내부 탐색, Order 엔티티 안의 User 객체의 userId가 파라미터 userId와 일치하는지 검사 
	// GroupOrderIsNull : groupOrder의 id값이 NULL
}
