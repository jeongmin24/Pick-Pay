package com.ssafy.pickpay.util;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.ssafy.pickpay.dto.GroupDutchPaymentRequestedEvent;
import com.ssafy.pickpay.service.NotificationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GroupDutchPaymentNotificationListener {
	
	private final NotificationService notificationService;
	
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDutchPaymentRequested(GroupDutchPaymentRequestedEvent event) {
        for (var request : event.requests()) {
            notificationService.sendDutchPaymentRequest(
                    request.userId(),
                    event.groupId(),
                    request.orderNo(),
                    request.amount()
            );
        }
    }

}
