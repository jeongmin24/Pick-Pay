package com.ssafy.pickpay.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ssafy.pickpay.domain.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

	Optional<Payment> findByPaymentKey(String paymentKey);

    Optional<Payment> findByOrder_OrderId(Long orderId);
}
