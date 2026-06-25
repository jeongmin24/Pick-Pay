package com.ssafy.pickpay.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ssafy.pickpay.domain.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

	Optional<Payment> findByPaymentKey(String paymentKey);

    Optional<Payment> findByOrder_OrderId(Long orderId);

    @Query("""
        select p
        from Payment p
        join fetch p.order o
        join o.groupOrder g
        where g.groupId = :groupId
    """)
    List<Payment> findByGroupId(@Param("groupId") String groupId);
}
