package com.ssafy.pickpay.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import com.ssafy.pickpay.common.OrderStatus;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
		name = "orders", // 예약어 충돌 방지
		indexes = {
				@Index(name = "idx_orders_order_no", columnList = "order_no")
		}
) 
public class Order {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id") // Nullable
    private GroupOrder groupOrder;
    
    @Column(name = "order_no", length = 64, unique = true)
    private String orderNo; // PG사용 orderId 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private Long totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    private OrderStatus status;

    @CreationTimestamp
    private LocalDateTime createdAt;
    
    // 주문서 생성 메서드 
    public static Order createOrder(GroupOrder groupOrder, User user) {
        Order order = new Order();
        order.groupOrder = groupOrder;
        order.user = user;
        order.totalPrice = 0L; // 초기값 설정 (나중에 계산 후 업데이트)
        order.status = OrderStatus.PENDING; // 결제 대기 상태
        return order;
    }
    
    public void assignOrderNo(String orderNo) {
    	if(this.orderNo != null) {
    		throw new IllegalStateException("이미 주문번호가 발급된 주문입니다.");
    	}
    	this.orderNo = orderNo;
    }
    
    public void updateTotalPrice(Long totalPrice) {
        this.totalPrice = totalPrice;
    }
    
    public void markPaid() {
    	if(this.status == OrderStatus.PAID) {
    		return;
    	}
    	if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("결제 대기 상태의 주문만 결제 완료 처리할 수 있습니다.");
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