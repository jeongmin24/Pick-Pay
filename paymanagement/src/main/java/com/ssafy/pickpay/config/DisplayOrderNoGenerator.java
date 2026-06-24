package com.ssafy.pickpay.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.ssafy.pickpay.domain.Order;
import com.ssafy.pickpay.repository.OrderRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DisplayOrderNoGenerator {

    private static final int DISPLAY_ORDER_NO_WIDTH = 3;

    private final OrderRepository orderRepository;

    public String generate(LocalDate orderDate) {
        if (orderDate == null) {
            throw new IllegalArgumentException("orderDate is required to generate displayOrderNo.");
        }

        LocalDateTime startOfDay = orderDate.atStartOfDay();
        LocalDateTime startOfNextDay = orderDate.plusDays(1).atTime(LocalTime.MIDNIGHT);

        int nextSequence = orderRepository
                .findDisplayOrderNoCandidatesForUpdate(startOfDay, startOfNextDay)
                .stream()
                .map(Order::getDisplayOrderNo)
                .mapToInt(this::parseDisplayOrderNo)
                .max()
                .orElse(0) + 1;

        return String.format(Locale.ROOT, "%0" + DISPLAY_ORDER_NO_WIDTH + "d", nextSequence);
    }

    private int parseDisplayOrderNo(String displayOrderNo) {
        if (displayOrderNo == null || displayOrderNo.isBlank()) {
            return 0;
        }

        try {
            return Integer.parseInt(displayOrderNo);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid displayOrderNo format: " + displayOrderNo, e);
        }
    }
}
