package com.ssafy.pickpay.service;

import org.springframework.stereotype.Service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.ssafy.pickpay.domain.User;
import com.ssafy.pickpay.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {
	
	private final UserRepository userRepository;
	
    public void sendDutchPaymentRequest(
            Long userId,
            Long groupId,
            String orderNo,
            Long amount
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow();

        if (user.getFcmToken() == null || user.getFcmToken().isBlank()) {
            return;
        }

        Message message = Message.builder()
                .setToken(user.getFcmToken())
                .setNotification(Notification.builder()
                        .setTitle("정산 요청")
                        .setBody("단체 주문 정산을 진행해주세요.")
                        .build())
                .putData("type", "GROUP_DUTCH_PAYMENT_REQUEST")
                .putData("groupId", String.valueOf(groupId))
                .putData("orderNo", orderNo)
                .putData("amount", String.valueOf(amount))
                .build();

        FirebaseMessaging.getInstance().sendAsync(message);
    }

}
