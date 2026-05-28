package com.ssafy.pickpay.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.ssafy.pickpay.domain.GroupOrder;
import com.ssafy.pickpay.domain.Menu;
import com.ssafy.pickpay.domain.Order;
import com.ssafy.pickpay.domain.OrderItems;
import com.ssafy.pickpay.domain.User;
import com.ssafy.pickpay.dto.FirebaseCartItemDTO;
import com.ssafy.pickpay.dto.GroupOrderRequestDTO;
import com.ssafy.pickpay.repository.GroupOrderRepository;
import com.ssafy.pickpay.repository.MenuRepository;
import com.ssafy.pickpay.repository.OrderItemsRepository;
import com.ssafy.pickpay.repository.OrderRepository;
import com.ssafy.pickpay.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GroupOrderService {
	
	private final GroupOrderRepository groupOrderRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderItemsRepository orderItemsRepository;
    private final MenuRepository menuRepository;
    private final FirebaseSyncService firebaseSyncService;
	
	// 그룹 주문 세선 생성
    @Transactional
    public GroupOrder createSession(String loginId) {
  
    	// 로그인한 사용자의 loginId -> DB에서 방장할 유저 조회 
        User host = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        // RDB에 방 정보 저장
        GroupOrder groupOrder = GroupOrder.createGroupOrder(host);
        GroupOrder savedOrder = groupOrderRepository.save(groupOrder);

        // firebase에 실시간 노드 생성 
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("group_orders/" + savedOrder.getGroupId());

        Map<String, Object> initialData = new HashMap<>();
        initialData.put("status", "OPEN");
        initialData.put("hostId", host.getUserId()); // 유저의 PK 숫자를 Firebase에 기입 
        
        // 비동기로 안전하게 쓰기
        ref.setValueAsync(initialData);
        

        return savedOrder;
    }
	
	// 초대 링크 접속시 그룹 정보 확인
	
	// 방장 주문 마감 처리 
    @Transactional
    public void closeSession(Long groupId, String payType) {
    	GroupOrder groupOrder = groupOrderRepository.findById(groupId)
    			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 그룹입니다."));
    	groupOrder.closeAndSetPayType(payType);
    	
    	try {
            // 2) Firebase에서 데이터 수신
            List<FirebaseCartItemDTO> firebaseItems = firebaseSyncService.getCartItems(groupId.toString());
            
            if (firebaseItems.isEmpty()) {
                throw new IllegalStateException("장바구니가 비어 있어 마감할 수 없습니다.");
            }

            // 3) 데이터를 userId를 기준으로 그룹화 (Map<Long, List<FirebaseCartItemDto>>)
            Map<Long, List<FirebaseCartItemDTO>> itemsByUser = firebaseItems.stream()
                    .collect(Collectors.groupingBy(FirebaseCartItemDTO::getUserId));

            // 4) 각 유저별로 Order(주문서)와 OrderItems(메세 내역) 생성
            for (Map.Entry<Long, List<FirebaseCartItemDTO>> entry : itemsByUser.entrySet()) {
                Long userId = entry.getKey();
                List<FirebaseCartItemDTO> userCartItems = entry.getValue();

                // 유저 검증
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다. ID: " + userId));

                // Order 생성 및 영속화 (먼저 저장되어야 OrderItems가 참조할 ID가 생김)
                Order order = Order.createOrder(groupOrder, user);
                orderRepository.save(order);

                long totalPrice = 0L;
                List<OrderItems> orderItemsList = new ArrayList<>();

                // 유저가 담은 개별 메뉴들 처리
                for (FirebaseCartItemDTO dto : userCartItems) {
                    Menu product = menuRepository.findById(dto.getProductId())
                            .orElseThrow(() -> new IllegalArgumentException("메뉴를 찾을 수 없습니다. ID: " + dto.getProductId()));

                    OrderItems orderItem = OrderItems.createOrderItem(order, product, dto.getQuantity());
                    orderItemsList.add(orderItem);

                    // 총액 누적 (단가 * 수량)
                    totalPrice += (product.getPrice() * dto.getQuantity());
                }

                // OrderItems 일괄 저장
                orderItemsRepository.saveAll(orderItemsList);

                // 계산된 총액을 Order에 업데이트 (JPA 더티체킹으로 자동 UPDATE 쿼리 발생)
                order.setTotalPrice(totalPrice);
            }

            // 5) Firebase 실시간 상태 업데이트 (클라이언트 화면 전환용)
            DatabaseReference groupRef = FirebaseDatabase.getInstance()
                    .getReference("group_orders/" + groupId);

            Map<String, Object> updates = new HashMap<>();
            updates.put("status", "LOCKED");
            updates.put("payType", payType);

            groupRef.updateChildrenAsync(updates);

        } catch (Exception e) {
        	System.err.println("마감 에러 원인: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("주문 마감 및 결제 방식 설정 중 오류가 발생했습니다.", e);
        }
    }
}
