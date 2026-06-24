package com.ssafy.pickpay.domain;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.ssafy.pickpay.common.OrderStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_orders_order_no", columnList = "order_no")
        }
)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private GroupOrder groupOrder;

    @Column(name = "order_no", length = 64, unique = true)
    private String orderNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private Long totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    private OrderStatus status;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public static Order createOrder(GroupOrder groupOrder, User user) {
        Order order = new Order();
        order.groupOrder = groupOrder;
        order.user = user;
        order.totalPrice = 0L;
        order.status = OrderStatus.PENDING;
        return order;
    }

    public void assignOrderNo(String orderNo) {
        if (this.orderNo != null) {
            throw new IllegalStateException("Order number has already been assigned.");
        }
        this.orderNo = orderNo;
    }

    public void updateTotalPrice(Long totalPrice) {
        this.totalPrice = totalPrice;
    }

    public void markPaymentApproved() {
        if (this.status == OrderStatus.PAYMENT_APPROVED || this.status == OrderStatus.PAID) {
            return;
        }
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("Only pending orders can be approved for group payment.");
        }
        this.status = OrderStatus.PAYMENT_APPROVED;
    }

    public void markPaid() {
        if (this.status == OrderStatus.PAID) {
            return;
        }
        if (this.status != OrderStatus.PENDING && this.status != OrderStatus.PAYMENT_APPROVED) {
            throw new IllegalStateException("Only pending or group-approved orders can be paid.");
        }
        this.status = OrderStatus.PAID;
    }

    public void markPaymentFailed() {
        this.status = OrderStatus.PAYMENT_FAILED;
    }

    public void cancel() {
        this.status = OrderStatus.CANCELLED;
    }

    public boolean isPaid() {
        return this.status == OrderStatus.PAID;
    }
}
