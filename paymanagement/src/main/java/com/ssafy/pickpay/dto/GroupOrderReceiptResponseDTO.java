package com.ssafy.pickpay.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupOrderReceiptResponseDTO {

    private String groupId;
    private String payType;
    private String groupStatus;
    private Long totalGroupPrice;
    private List<UserReceiptDTO> userReceipts;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserReceiptDTO {
        private Long userId;
        private String orderNo;
        private String displayOrderNo;
        private String nickname;
        private String orderStatus;
        private Long userTotalPrice;
        private List<OrderReceiptItemDTO> items;
    }
}
