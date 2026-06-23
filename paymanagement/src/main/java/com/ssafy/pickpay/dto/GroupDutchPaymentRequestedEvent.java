package com.ssafy.pickpay.dto;

import java.util.List;

import com.ssafy.pickpay.domain.Order;

public record GroupDutchPaymentRequestedEvent(
		Long groupId,
		List<DutchPaymentRequest> requests
		) {

	public record DutchPaymentRequest(
            Long userId,
            String orderNo,
            Long amount
    ) {}
	
	public static GroupDutchPaymentRequestedEvent from(Long groupId, List<Order> orders) {
        return new GroupDutchPaymentRequestedEvent(
                groupId,
                orders.stream()
                        .map(order -> new DutchPaymentRequest(
                                order.getUser().getUserId(),
                                order.getOrderNo(),
                                order.getTotalPrice()
                        ))
                        .toList()
        );
    }
}
