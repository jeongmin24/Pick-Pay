package com.ssafy.pickpay.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.ssafy.pickpay.common.GroupOrderStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long groupId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id")
    private User host;

    @Column(nullable = false, unique = true, length = 100)
    private String shareToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private GroupOrderStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pickup_user_id")
    private User pickupUser;

    private String payType;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public static GroupOrder createGroupOrder(User host) {
        GroupOrder groupOrder = new GroupOrder();
        groupOrder.setHost(host);
        groupOrder.setStatus(GroupOrderStatus.OPEN);
        groupOrder.setShareToken(UUID.randomUUID().toString());
        return groupOrder;
    }

    public void closeAndSetPayType(String payType) {
        if (this.status != GroupOrderStatus.OPEN) {
            throw new IllegalStateException("Only open group orders can be closed.");
        }

        this.status = GroupOrderStatus.LOCKED;
        this.payType = payType;
    }

    public void markPaid() {
        if (this.status == GroupOrderStatus.PAID) {
            return;
        }

        if (this.status != GroupOrderStatus.LOCKED) {
            throw new IllegalStateException("Only locked group orders can be marked as paid.");
        }

        this.status = GroupOrderStatus.PAID;
    }
}
