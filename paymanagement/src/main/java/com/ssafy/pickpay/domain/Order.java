package com.ssafy.pickpay.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "orders") // 예약어 충돌 방지
public class Order {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id") // Nullable
    private GroupOrder groupOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private Long totalPrice;

    @Column(length = 50)
    private String status;

    @CreationTimestamp
    private LocalDateTime createdAt;
    
    // 주문서 생성 메서드 
    public static Order createOrder(GroupOrder groupOrder, User user) {
        Order order = new Order();
        order.setGroupOrder(groupOrder);
        order.setUser(user);
        order.setTotalPrice(0L); // 초기값 설정 (나중에 계산 후 업데이트)
        order.setStatus("WAITING_PAYMENT"); // 결제 대기 상태
        return order;
    }
}