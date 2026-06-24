package com.ssafy.pickpay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.ssafy.pickpay.common.GroupOrderStatus;
import com.ssafy.pickpay.common.GroupPayType;
import com.ssafy.pickpay.domain.GroupOrder;
import com.ssafy.pickpay.domain.Menu;
import com.ssafy.pickpay.domain.Order;
import com.ssafy.pickpay.domain.OrderItems;
import com.ssafy.pickpay.domain.User;
import com.ssafy.pickpay.dto.FirebaseCartItemDTO;
import com.ssafy.pickpay.repository.GroupOrderRepository;
import com.ssafy.pickpay.repository.MenuRepository;
import com.ssafy.pickpay.repository.OrderItemsRepository;
import com.ssafy.pickpay.repository.OrderRepository;
import com.ssafy.pickpay.repository.UserRepository;
import com.ssafy.pickpay.service.FirebaseSyncService;
import com.ssafy.pickpay.service.GroupOrderService;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@SpringBootTest
@Transactional
class GroupOrderServiceIntegrationTest {

    @Autowired private GroupOrderService groupOrderService;
    @Autowired private GroupOrderRepository groupOrderRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private MenuRepository menuRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemsRepository orderItemsRepository;
    @Autowired private EntityManager em;

    @MockitoBean
    private FirebaseSyncService firebaseSyncService;

    @Test
    @DisplayName("Dutch close creates one pending payment order per user")
    void closeSessionWithDutchPayCreatesUserOrders() throws Exception {
        User host = User.builder()
                .loginId("host")
                .password("1234")
                .nickname("host")
                .build();
        User member = User.builder()
                .loginId("member1")
                .password("1234")
                .nickname("member")
                .build();
        userRepository.save(host);
        userRepository.save(member);

        GroupOrder groupOrder = GroupOrder.createGroupOrder(host);
        groupOrderRepository.save(groupOrder);

        Menu menu1 = Menu.builder()
                .name("americano")
                .price(20_000L)
                .isActive(true)
                .build();
        Menu menu2 = Menu.builder()
                .name("latte")
                .price(3_000L)
                .isActive(true)
                .build();
        menuRepository.save(menu1);
        menuRepository.save(menu2);

        List<FirebaseCartItemDTO> mockFirebaseCart = List.of(
                new FirebaseCartItemDTO(host.getUserId(), menu1.getName(), menu1.getMenuId(), 1),
                new FirebaseCartItemDTO(member.getUserId(), menu1.getName(), menu1.getMenuId(), 1),
                new FirebaseCartItemDTO(member.getUserId(), menu2.getName(), menu2.getMenuId(), 2)
        );

        given(firebaseSyncService.getCartItems(groupOrder.getGroupId()))
                .willReturn(mockFirebaseCart);

        groupOrderService.closeSession(
                host.getUserId(),
                groupOrder.getGroupId(),
                GroupPayType.DUTCH
        );

        em.flush();
        em.clear();

        GroupOrder updatedGroup = groupOrderRepository.findById(groupOrder.getGroupId())
                .orElseThrow();
        assertThat(updatedGroup.getStatus()).isEqualTo(GroupOrderStatus.PAYMENT_PENDING);
        assertThat(updatedGroup.getPayType()).isEqualTo(GroupPayType.DUTCH);

        List<Order> orders = orderRepository.findAll();
        assertThat(orders).hasSize(2);

        Order hostOrder = orders.stream()
                .filter(order -> order.getUser().getUserId().equals(host.getUserId()))
                .findFirst()
                .orElseThrow();
        Order memberOrder = orders.stream()
                .filter(order -> order.getUser().getUserId().equals(member.getUserId()))
                .findFirst()
                .orElseThrow();

        assertThat(hostOrder.getTotalPrice()).isEqualTo(20_000L);
        assertThat(memberOrder.getTotalPrice()).isEqualTo(26_000L);

        List<OrderItems> orderItems = orderItemsRepository.findAll();
        assertThat(orderItems).hasSize(3);
    }
}
