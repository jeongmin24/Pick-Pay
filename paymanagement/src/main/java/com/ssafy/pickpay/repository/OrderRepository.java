package com.ssafy.pickpay.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ssafy.pickpay.common.OrderStatus;
import com.ssafy.pickpay.domain.Order;

import jakarta.persistence.LockModeType;


public interface OrderRepository extends JpaRepository<Order, Long>{
	List<Order> findByGroupOrder_GroupId(String groupId);
	List<Order> findByUser_UserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
	List<Order> findByUser_UserIdAndGroupOrderIsNullOrderByCreatedAtDesc(Long userId, Pageable pageable);
	Optional<Order> findByOrderIdAndUser_UserIdAndGroupOrderIsNull(Long orderId, Long userId); //groupOrderIsNull조건 -> 개별 주문 영수증 
	/**
	 	findBy : SELECT * FROM Order WHERE order_id = ? AND user_id = ? AND group_order_id IS NULL
		Q : SELECT o.* FROM orders o JOIN users u o.user_id = u.user_id WHERE o.order_id = ? AND u.user_id = ? AND o.group_order_id IS NULL;
		User_UserId: _ = 객체 내부 탐색, Order 엔티티 안의 User 객체의 userId가 파라미터 userId와 일치하는지 검사 
		GroupOrderIsNull : groupOrder의 id값이 NULL
	 * */
	
	// orderNo 조회용 PG사에서 넘어온 orderId(orderNo) 기준으로 주문을 찾음 
	Optional<Order> findByOrderNo(String orderNo);
	Optional<Order> findByOrderNoAndUser_UserId(String orderNo, Long userId);
	Optional<Order> findByOrderNoAndUser_UserIdAndGroupOrderIsNull(String orderNo, Long userId);

	@Query("""
		select o
		from Order o
		join fetch o.user u
		left join fetch o.groupOrder g
		where o.orderNo = :orderNo
	""")
	Optional<Order> findByOrderNoWithUserAndGroupOrder(@Param("orderNo") String orderNo);
	
	@Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select o
        from Order o
        join fetch o.user u
        left join fetch o.groupOrder g
        where o.orderNo = :orderNo
    """)
    Optional<Order> findByOrderNoForUpdate(@Param("orderNo") String orderNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select o
        from Order o
        join fetch o.user u
        join fetch o.groupOrder g
        where g.groupId = :groupId
    """)
    List<Order> findByGroupOrderGroupIdForUpdate(@Param("groupId") String groupId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select o
        from Order o
        where o.createdAt >= :startOfDay
          and o.createdAt < :startOfNextDay
          and o.displayOrderNo is not null
    """)
    List<Order> findDisplayOrderNoCandidatesForUpdate(
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("startOfNextDay") LocalDateTime startOfNextDay
    );
	
	boolean existsByGroupOrder_GroupIdAndStatusNot(
            String groupId,
            OrderStatus status
    );
	
	List<Order> findByUser_UserIdAndGroupOrderIsNullAndStatusOrderByCreatedAtDesc(
	        Long userId,
	        OrderStatus status,
	        Pageable pageable
	);
}
