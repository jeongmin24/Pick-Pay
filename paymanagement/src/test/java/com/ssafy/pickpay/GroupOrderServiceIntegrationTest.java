package com.ssafy.pickpay;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

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
import static org.mockito.BDDMockito.given;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@SpringBootTest
@Transactional
public class GroupOrderServiceIntegrationTest {
	
	@Autowired private GroupOrderService groupOrderService;
    @Autowired private GroupOrderRepository groupOrderRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private MenuRepository menuRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemsRepository orderItemsRepository;
    
    @Autowired private EntityManager em; // 더티 체킹 검증용
    
    @MockitoBean
    private FirebaseSyncService firebaseSyncService;
    
    @Test
    @DisplayName("주문 마감 통합 테스트")
    void closeSessionTest() throws Exception {
    	User host = User.builder().loginId("host").password("1234").nickname("방장").build();
        User member = User.builder().loginId("member1").password("1234").nickname("팀원").build();
        userRepository.save(host);
        userRepository.save(member);

        GroupOrder groupOrder = GroupOrder.createGroupOrder(host);
        groupOrderRepository.save(groupOrder);

        Menu menu1 = new Menu(); menu1.setName("아이스아메리카노"); menu1.setPrice(20000L); menu1.setIsActive(true);
        Menu menu2 = new Menu(); menu2.setName("아이스카페라떼"); menu2.setPrice(3000L); menu2.setIsActive(true);
        menuRepository.save(menu1);
        menuRepository.save(menu2);

        // Firebase에서 반환될 가짜 장바구니 데이터 정의
        // 방장이 아이스아메리카노 2개, 멤버가 아이스카페라떼 1개 담았다고 가정
        List<FirebaseCartItemDTO> mockFirebaseCart = List.of(
                new FirebaseCartItemDTO(host.getUserId(), menu1.getMenuId(), 1),
                new FirebaseCartItemDTO(member.getUserId(), menu1.getMenuId(), 1),
                new FirebaseCartItemDTO(member.getUserId(), menu2.getMenuId(), 2)
        );
        
        // firebaseSyncService.getCartItems() 호출 시 가짜 리스트를 반환
        given(firebaseSyncService.getCartItems(groupOrder.getGroupId().toString()))
                .willReturn(mockFirebaseCart);

        // 마감 로직 실행
        groupOrderService.closeSession(groupOrder.getGroupId(), "더치페이");

        em.flush();
        em.clear();

        
        GroupOrder updatedGroup = groupOrderRepository.findById(groupOrder.getGroupId()).orElseThrow();
        assertThat(updatedGroup.getStatus()).isEqualTo("LOCKED");
        assertThat(updatedGroup.getPayType()).isEqualTo("더치페이");

        List<Order> orders = orderRepository.findAll();
        assertThat(orders).hasSize(2);

        Order hostOrder = orders.stream().filter(o -> o.getUser().getUserId().equals(host.getUserId())).findFirst().orElseThrow();
        Order memberOrder = orders.stream().filter(o -> o.getUser().getUserId().equals(member.getUserId())).findFirst().orElseThrow();
        
        assertThat(hostOrder.getTotalPrice()).isEqualTo(20000L); 
        assertThat(memberOrder.getTotalPrice()).isEqualTo(26000L); 

        List<OrderItems> orderItems = orderItemsRepository.findAll();
        assertThat(orderItems).hasSize(3);
    }

}
