package com.ssafy.pickpay.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupOrder {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long groupId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id")
    private User host;

    @Column(unique = true)
    private String shareLink;

    @Column(length = 50)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pickup_user_id") // Nullable 자동 적용
    private User pickupUser;

    private String payType;

    @CreationTimestamp
    private LocalDateTime createdAt;
    
    // 생성 팩토리 메서드 
    public static GroupOrder createGroupOrder(User host) {
        GroupOrder groupOrder = new GroupOrder();
        groupOrder.setHost(host);
        groupOrder.setStatus("OPEN");
        groupOrder.setShareLink(UUID.randomUUID().toString()); // 초대 링크용 고유 UUID 생성 -> 도메인 주소를 포함해서 링크를 생성할것 
        return groupOrder;
    }
    
    // 주문 마감 및 결제 방식 확정
    public void closeAndSetPayType(String payType) {
        if (!"OPEN".equals(this.status)) {
            throw new IllegalStateException("이미 마감되었거나 종료된 주문 세션입니다.");
        }
        this.status = "LOCKED";
        this.payType = payType; // 결제 방식 확정 
    }
}