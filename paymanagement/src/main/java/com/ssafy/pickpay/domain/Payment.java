package com.ssafy.pickpay.domain;

import java.time.LocalDateTime;

import com.ssafy.pickpay.common.PaymentStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(nullable = false, unique = true)
    private String paymentKey;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PaymentStatus status;

    private LocalDateTime approvedAt;

    private String failReason;

    public static Payment approving(Order order, String paymentKey, Long amount) {
        Payment payment = new Payment();
        payment.order = order;
        payment.paymentKey = paymentKey;
        payment.amount = amount;
        payment.status = PaymentStatus.APPROVING;
        return payment;
    }

    public void approvePendingGroup() {
        this.status = PaymentStatus.APPROVED_PENDING_GROUP;
        this.approvedAt = LocalDateTime.now();
    }

    public void approve() {
        this.status = PaymentStatus.APPROVED;
        this.approvedAt = LocalDateTime.now();
    }

    public void cancel(String reason) {
        this.status = PaymentStatus.CANCELED;
        this.failReason = reason;
    }

    public void fail(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failReason = reason;
    }
}
