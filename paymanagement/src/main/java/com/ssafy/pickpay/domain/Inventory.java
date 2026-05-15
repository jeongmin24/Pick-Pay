package com.ssafy.pickpay.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
public class Inventory {
    @Id
    private Long menuId; // Menu의 PK를 FK로 사용

    @MapsId
    @OneToOne
    @JoinColumn(name = "menu_id")
    private Menu menu;

    private Integer stockQuantity;
}
