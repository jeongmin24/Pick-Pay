package com.ssafy.pickpay.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.ssafy.pickpay.common.GroupOrderStatus;
import com.ssafy.pickpay.common.GroupPayType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
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
    @Column(name = "group_id", nullable = false, updatable = false, length = 36)
    private String groupId;

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

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private GroupPayType payType;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public static GroupOrder createGroupOrder(User host) {
        GroupOrder groupOrder = new GroupOrder();
        groupOrder.setGroupId(UUID.randomUUID().toString());
        groupOrder.setHost(host);
        groupOrder.setStatus(GroupOrderStatus.OPEN);
        groupOrder.setShareToken(UUID.randomUUID().toString());
        return groupOrder;
    }

    public void closeAndSetPayType(GroupPayType payType) {
        if (this.status != GroupOrderStatus.OPEN) {
            throw new IllegalStateException("Group order is already closed.");
        }

        this.status = payType == GroupPayType.DUTCH
                ? GroupOrderStatus.PAYMENT_PENDING
                : GroupOrderStatus.LOCKED;
        this.payType = payType;
    }

    public void markPaid() {
        if (this.status == GroupOrderStatus.PAID) {
            return;
        }

        if (this.status != GroupOrderStatus.LOCKED
                && this.status != GroupOrderStatus.PAYMENT_PENDING) {
            throw new IllegalStateException("Only locked or payment-pending groups can be paid.");
        }

        this.status = GroupOrderStatus.PAID;
    }

    public void markPaymentFailed() {
        if (this.status == GroupOrderStatus.PAYMENT_FAILED) {
            return;
        }
        this.status = GroupOrderStatus.PAYMENT_FAILED;
    }
}
