package com.ssafy.pickpay.dto;

import com.ssafy.pickpay.domain.Menu;

import lombok.Getter;

@Getter
public class MenuResponseDTO {
	private final Long menuId;
    private final String name;
    private final Long price;
    private final String imageUrl;
    private final Integer stockQuantity;

    public MenuResponseDTO(Menu menu, String baseUrl) {
        this.menuId = menu.getMenuId();
        this.name = menu.getName();
        this.price = menu.getPrice();
        this.imageUrl = baseUrl + "/images/" + menu.getImageUrl();
        this.stockQuantity = menu.getStockQuantity();
    }
}
