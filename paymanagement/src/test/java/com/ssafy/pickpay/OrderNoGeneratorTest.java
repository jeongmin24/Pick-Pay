package com.ssafy.pickpay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.ssafy.pickpay.config.OrderNoGenerator;

class OrderNoGeneratorTest {

    private final OrderNoGenerator orderNoGenerator = new OrderNoGenerator();

    @Test
    void generateAddsRandomSuffixToOrderIdHash() {
        String firstOrderNo = orderNoGenerator.generate(1L);
        String secondOrderNo = orderNoGenerator.generate(1L);

        assertThat(firstOrderNo).matches("ORD-\\d{8}-[A-Za-z0-9]{8,}-[0-9A-Z]{8}");
        assertThat(secondOrderNo).matches("ORD-\\d{8}-[A-Za-z0-9]{8,}-[0-9A-Z]{8}");
        assertThat(secondOrderNo).isNotEqualTo(firstOrderNo);
    }

    @Test
    void decodeOrderIdSupportsCurrentAndLegacyFormats() {
        String orderNo = orderNoGenerator.generate(123L);
        String legacyOrderNo = orderNo.substring(0, orderNo.lastIndexOf('-'));

        assertThat(orderNoGenerator.decodeOrderId(orderNo)).isEqualTo(123L);
        assertThat(orderNoGenerator.decodeOrderId(legacyOrderNo)).isEqualTo(123L);
    }

    @Test
    void decodeOrderIdRejectsInvalidFormat() {
        assertThatThrownBy(() -> orderNoGenerator.decodeOrderId("BAD-20260624-ABCDEFGH-12345678"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> orderNoGenerator.decodeOrderId("ORD-20260624"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
