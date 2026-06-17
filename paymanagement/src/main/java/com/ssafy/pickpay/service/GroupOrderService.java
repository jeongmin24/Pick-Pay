package com.ssafy.pickpay.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.ssafy.pickpay.domain.GroupOrder;
import com.ssafy.pickpay.domain.Menu;
import com.ssafy.pickpay.domain.Order;
import com.ssafy.pickpay.domain.OrderItems;
import com.ssafy.pickpay.domain.User;
import com.ssafy.pickpay.dto.CartItemRequest;
import com.ssafy.pickpay.dto.FirebaseCartItemDTO;
import com.ssafy.pickpay.dto.GroupJoinResponseDTO;
import com.ssafy.pickpay.dto.GroupOrderReceiptResponseDTO;
import com.ssafy.pickpay.dto.GroupOrderReceiptResponseDTO.UserReceiptDTO;
import com.ssafy.pickpay.dto.OrderReceiptItemDTO;
import com.ssafy.pickpay.repository.GroupOrderRepository;
import com.ssafy.pickpay.repository.MenuRepository;
import com.ssafy.pickpay.repository.OrderItemsRepository;
import com.ssafy.pickpay.repository.OrderRepository;
import com.ssafy.pickpay.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GroupOrderService {
	
	private final OrderService orderService;
	private final GroupOrderRepository groupOrderRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderItemsRepository orderItemsRepository;
    private final MenuRepository menuRepository;
    private final FirebaseSyncService firebaseSyncService;
	
	// 그룹 주문 세선 생성
    @Transactional
    public GroupOrder createSession(Long userId) {
    	
    	// 이미 유저가 만든 OPEN 방이 있는지 확인
    	Optional<GroupOrder> existingGroupOrder =
    			groupOrderRepository.findByHost_UserIdAndStatus(userId, "OPEN");
    	if(existingGroupOrder.isPresent()) {
    		return existingGroupOrder.get(); // 이미 방이 있으면 기존 방 반환
    	}
  
    	// 로그인한 사용자의 userId(PK) -> DB에서 방장할 유저 조회 
        User host = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        // RDB에 방 정보 저장
        GroupOrder groupOrder = GroupOrder.createGroupOrder(host);
        GroupOrder savedOrder = groupOrderRepository.save(groupOrder);

        // firebase에 실시간 노드 생성 
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("group_orders/" + savedOrder.getGroupId());

        Map<String, Object> initialData = new HashMap<>();
        initialData.put("status", "OPEN");
        initialData.put("hostId", userId); // 유저의 PK 숫자를 Firebase에 기입 
        
        // 비동기로 안전하게 쓰기
        ref.setValueAsync(initialData);
        

        return savedOrder;
    }
	
	// shareToken으로 GroupOrder 조회 후 방장 여부 계산
    @Transactional(readOnly = true)
    public GroupJoinResponseDTO joinGroup(Long userId, String shareToken) {
    	GroupOrder groupOrder = groupOrderRepository.findByShareToken(shareToken)
    			.orElseThrow(() -> new IllegalArgumentException("유효하지 않은 초대링크 입니다."));
    	
    	String status = groupOrder.getStatus();
    	
    	if("PAID".equals(status)) {
    		throw new IllegalStateException("이미 결제가 완료된 그룹방입니다.");
    	}
    	
    	if ("LOCKED".equals(status)) {
            throw new IllegalStateException("이미 주문이 마감된 그룹방입니다.");
        }
    	
    	boolean isHost = groupOrder.getHost().getUserId().equals(userId);
    	
    	return new GroupJoinResponseDTO(
    			groupOrder.getGroupId(),
    			groupOrder.getStatus(),
    			isHost);
    }
	
	// 방장 주문 마감 처리 
    @Transactional
    public void closeSession(Long groupId, String payType) {
    	
    	GroupOrder groupOrder = groupOrderRepository.findById(groupId)
    			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 그룹입니다."));
    	
    	if("PAID".equals(groupOrder.getStatus())) {
    		throw new IllegalStateException("이미 결제가 완료된 그룹방입니다.");
    	}
    	
    	if ("LOCKED".equals(groupOrder.getStatus())) {
            throw new IllegalStateException("이미 주문이 마감된 그룹방입니다.");
        }
    	
    	groupOrder.closeAndSetPayType(payType);
    	
    	try {
            List<FirebaseCartItemDTO> firebaseItems = firebaseSyncService.getCartItems(groupId.toString()); // items 긁어오기
            
            if (firebaseItems.isEmpty()) {
                throw new IllegalStateException("장바구니가 비어 있어 마감할 수 없습니다.");
            }

            // 1: [ ] / 2: [ ] 형식으로 분류 
            Map<Long, List<FirebaseCartItemDTO>> itemsByUser = firebaseItems.stream()
                    .collect(Collectors.groupingBy(FirebaseCartItemDTO::getUserId)); // 리스트에 있는 아이템을 userId 기준으로 그룹화 

            // User 마다 가진 장바구니 아이템을 DB에 하나씩 저장
            for (Map.Entry<Long, List<FirebaseCartItemDTO>> entry : itemsByUser.entrySet()) {
                Long userId = entry.getKey();
                List<FirebaseCartItemDTO> userCartItems = entry.getValue();

                List<CartItemRequest> cartItems = userCartItems.stream()
                		.map(item -> new CartItemRequest(item.getProductId(), item.getQuantity()))
                		.collect(Collectors.toList());
                
                orderService.createOrder(userId, cartItems, groupId);
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
    
    // 영수증 조회 
    @Transactional(readOnly = true)
    public GroupOrderReceiptResponseDTO getReceipt(Long groupId) {
    	
    	GroupOrder groupOrder = groupOrderRepository.findById(groupId)
    			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 그룹입니다"));
    	
    	List<Order> orders = orderRepository.findByGroupOrder_GroupId(groupId);
    	
    	// 영수증 리스트, 그룹 price 
    	Long totalGroupPrice = 0L;
    	List<UserReceiptDTO> userReceiptList = new ArrayList<>();
    	
    	// 모든 주문서를 돌면서 영수증 상세 내역 채우기 
    	for(Order order : orders) {
    		totalGroupPrice += order.getTotalPrice();
    		List<OrderItems> rawOrderItems = orderItemsRepository.findByOrder_OrderId(order.getOrderId());
    		List<OrderReceiptItemDTO> itemDTOList = rawOrderItems.stream()
    				.map(item -> new OrderReceiptItemDTO( // record 기본 생성자 
    						item.getProduct().getName(), // Product = Menu
    						item.getQuantity(),
    						item.getProduct().getPrice()
    						))
    				.collect(Collectors.toList());
    		
    		UserReceiptDTO userReceiptDTO = UserReceiptDTO.builder()
    				.userId(order.getUser().getUserId())
    				.nickname(order.getUser().getNickname())
    				.userTotalPrice(order.getTotalPrice())
    				.items(itemDTOList)
    				.build();
    		
    		userReceiptList.add(userReceiptDTO);
    	}
    	
    	return GroupOrderReceiptResponseDTO.builder()
    			.groupId(groupOrder.getGroupId())
    			.payType(groupOrder.getPayType())
    			.totalGroupPrice(totalGroupPrice)
    			.userReceipts(userReceiptList)
    			.build();
    	
    	
    }
}
