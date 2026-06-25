package com.ssafy.pickpay.domain;


import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.ssafy.pickpay.common.ErrorCode;
import com.ssafy.pickpay.exception.BusinessException;

@Entity
@Getter @Setter
@Builder
@NoArgsConstructor @AllArgsConstructor
public class Menu {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long menuId;

    private String name;
    private Long price;
    @Column(columnDefinition = "TEXT")
    private String imageUrl;
    private Boolean isActive;

    private Integer stockQuantity;
    
    public void decreaseStock(int quantity) {
    	
    	if (quantity <= 0) {
            throw new IllegalArgumentException("차감 수량은 1 이상이어야 합니다.");
        }

        if (this.stockQuantity == null) {
            throw new BusinessException(ErrorCode.OUT_OF_STOCK);
        }
    	
    	int restStock = this.stockQuantity - quantity;
    	
    	if(restStock < 0) {
            throw new BusinessException(ErrorCode.OUT_OF_STOCK);
    	}
    	
    	this.stockQuantity = restStock;
    }
}
