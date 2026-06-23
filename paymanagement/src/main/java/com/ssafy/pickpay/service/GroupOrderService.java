package com.ssafy.pickpay.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.ssafy.pickpay.common.GroupOrderStatus;
import com.ssafy.pickpay.common.GroupPayType;
import com.ssafy.pickpay.domain.GroupOrder;
import com.ssafy.pickpay.domain.Menu;
import com.ssafy.pickpay.domain.Order;
import com.ssafy.pickpay.domain.OrderItems;
import com.ssafy.pickpay.domain.User;
import com.ssafy.pickpay.dto.CartItemRequest;
import com.ssafy.pickpay.dto.FirebaseCartItemDTO;
import com.ssafy.pickpay.dto.GroupDutchPaymentRequestedEvent;
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
    private final ApplicationEventPublisher eventPublisher;
	
	// 그룹 주문 세선 생성
    @Transactional
    public GroupOrder createSession(Long userId) {
    	
    	// 이미 유저가 만든 OPEN 방이 있는지 확인
    	Optional<GroupOrder> existingGroupOrder =
    			groupOrderRepository.findByHost_UserIdAndStatus(userId, GroupOrderStatus.OPEN);
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
        initialData.put("status", GroupOrderStatus.OPEN.name());
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
    	
    	GroupOrderStatus status = groupOrder.getStatus();
    	
    	if(status == GroupOrderStatus.PAID) {
    		throw new IllegalStateException("이미 결제가 완료된 그룹방입니다.");
    	}
    	
    	if (status == GroupOrderStatus.LOCKED) {
            throw new IllegalStateException("이미 주문이 마감된 그룹방입니다.");
        }
    	
    	boolean isHost = groupOrder.getHost().getUserId().equals(userId);
    	
    	return new GroupJoinResponseDTO(
    			groupOrder.getGroupId(),
    			groupOrder.getStatus().name(),
    			isHost);
    }
	
	// 방장 주문 마감 처리 
    @Transactional
    public void closeSession(Long requestUserId, Long groupId, GroupPayType payType) {

        GroupOrder groupOrder = groupOrderRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 그룹입니다."));

        validateCanCloseGroupOrder(groupOrder, requestUserId);

        List<FirebaseCartItemDTO> firebaseItems;
        try {
            firebaseItems = firebaseSyncService.getCartItems(groupId.toString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firebase 장바구니 조회 중 요청이 중단되었습니다.", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new RuntimeException("Firebase 장바구니 조회 중 오류가 발생했습니다.", e);
        } catch (Exception e) {
            throw new RuntimeException("Firebase 장바구니 조회 중 알 수 없는 오류가 발생했습니다.", e);
        }

        if (firebaseItems.isEmpty()) {
            throw new IllegalStateException("장바구니가 비어 있어 마감할 수 없습니다.");
        }

        Map<Long, List<FirebaseCartItemDTO>> itemsByUser = firebaseItems.stream()
                .collect(Collectors.groupingBy(FirebaseCartItemDTO::getUserId));

        List<Order> createdOrders = switch (payType) {
            case DUTCH -> createGroupOrdersByUser(groupId, itemsByUser);
            case HOST -> List.of(createGroupOrderForHost(requestUserId, groupId, firebaseItems));
        };

        groupOrder.closeAndSetPayType(payType);
        firebaseSyncService.updateFirebaseGroupStatus(groupId, payType);
        
        if(payType == GroupPayType.DUTCH) {
        	eventPublisher.publishEvent(
        			// 이벤트 DTO 
        			GroupDutchPaymentRequestedEvent.from(groupId, createdOrders)
        	);
        }
    }

    private List<Order> createGroupOrdersByUser(
            Long groupId,
            Map<Long, List<FirebaseCartItemDTO>> itemsByUser
    ) {
    	List<Order> createdOrders = new ArrayList<>();
    	
        for (Map.Entry<Long, List<FirebaseCartItemDTO>> entry : itemsByUser.entrySet()) {
            Long userId = entry.getKey();
            List<FirebaseCartItemDTO> userCartItems = entry.getValue();

            List<CartItemRequest> cartItems = userCartItems.stream()
                    .map(item -> new CartItemRequest(
                            item.getProductId(),
                            item.getQuantity()
                    ))
                    .toList();

            Order order = orderService.createOrder(userId, cartItems, groupId);
            createdOrders.add(order);
        }
        
        return createdOrders;
    }

    private Order createGroupOrderForHost(
            Long hostUserId,
            Long groupId,
            List<FirebaseCartItemDTO> firebaseItems
    ) {
        List<CartItemRequest> cartItems = firebaseItems.stream()
                .map(item -> new CartItemRequest(
                        item.getProductId(),
                        item.getQuantity()
                ))
                .toList();

        return orderService.createOrder(hostUserId, cartItems, groupId);
    }

    private void validateCanCloseGroupOrder(GroupOrder groupOrder, Long requestUserId) {

        if (!groupOrder.getHost().getUserId().equals(requestUserId)) {
            throw new AccessDeniedException("방장만 주문을 마감할 수 있습니다.");
        }

        if (groupOrder.getStatus() == GroupOrderStatus.PAID) {
            throw new IllegalStateException("이미 결제가 완료된 그룹방입니다.");
        }

        if (groupOrder.getStatus() == GroupOrderStatus.LOCKED) {
            throw new IllegalStateException("이미 주문이 마감된 그룹방입니다.");
        }

        if (groupOrder.getStatus() != GroupOrderStatus.OPEN) {
            throw new IllegalStateException("마감할 수 없는 그룹방 상태입니다.");
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
                    .orderNo(order.getOrderNo())
    				.nickname(order.getUser().getNickname())
    				.userTotalPrice(order.getTotalPrice())
    				.items(itemDTOList)
    				.build();
    		
    		userReceiptList.add(userReceiptDTO);
    	}
    	
    	return GroupOrderReceiptResponseDTO.builder()
    			.groupId(groupOrder.getGroupId())
                .payType(groupOrder.getPayType() == null ? null : groupOrder.getPayType().name())
    			.totalGroupPrice(totalGroupPrice)
    			.userReceipts(userReceiptList)
    			.build();
    	
    	
    }
}
