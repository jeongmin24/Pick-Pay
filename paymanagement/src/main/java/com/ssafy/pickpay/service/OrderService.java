package com.ssafy.pickpay.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.pickpay.config.OrderNoGenerator;
import com.ssafy.pickpay.domain.GroupOrder;
import com.ssafy.pickpay.domain.Menu;
import com.ssafy.pickpay.domain.Order;
import com.ssafy.pickpay.domain.OrderItems;
import com.ssafy.pickpay.domain.User;
import com.ssafy.pickpay.dto.CartItemRequest;
import com.ssafy.pickpay.dto.IndividualOrderCreateRequestDTO;
import com.ssafy.pickpay.dto.IndividualOrderCreateResponseDTO;
import com.ssafy.pickpay.dto.IndividualOrderReceiptResponseDTO;
import com.ssafy.pickpay.dto.OrderReceiptItemDTO;
import com.ssafy.pickpay.repository.GroupOrderRepository;
import com.ssafy.pickpay.repository.MenuRepository;
import com.ssafy.pickpay.repository.OrderItemsRepository;
import com.ssafy.pickpay.repository.OrderRepository;
import com.ssafy.pickpay.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {
	
	private final OrderRepository orderRepository;
	private final OrderItemsRepository orderItemsRepository;
    private final UserRepository userRepository;
    private final MenuRepository menuRepository;
    private final GroupOrderRepository groupOrderRepository;
    private final OrderNoGenerator orderNoGenerator;
    
    @Transactional
    public IndividualOrderCreateResponseDTO createIndividualOrder(
    		Long userId, 
    		IndividualOrderCreateRequestDTO request) {
    	
    	// 주문 생성
    	Order order = createOrder(userId, request.items(), null); 
    	
    	// DTO로 반환
    	return new IndividualOrderCreateResponseDTO(
    			order.getOrderNo(),
    			order.getTotalPrice(),
    			order.getStatus().name() // enum -> String
    			);
    	
    }
    
    /**
     * 주문 생성 
     * */
    @Transactional
    public Order createOrder(
    		Long userId,
    		List<CartItemRequest> items,
            String groupId) {
    	
    	if(items == null || items.isEmpty()) {
    		throw new IllegalArgumentException("주문 항목이 비어 있습니다.");
    	}
    	
    	User user = userRepository.findById(userId)
    			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
    	
    	GroupOrder groupOrder = null;
    	
    	if(groupId != null) { // 단체주문인 경우 
    		groupOrder = groupOrderRepository.findById(groupId)
    				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 단체 주문 방입니다."));
    	}
    	
    	Order order = Order.createOrder(groupOrder, user);
    	Order savedOrder = orderRepository.saveAndFlush(order); // INSERT
    	
    	String orderNo = orderNoGenerator.generate(savedOrder.getOrderId());
    	savedOrder.assignOrderNo(orderNo); // UPDATE
    	
    	Map<Long, Integer> mergedItems = mergeItems(items);
    	
    	// 전달받은 items를 반복문 돌면서 DB에 명시된 가격 확인 
    	long totalPrice = 0L;
    	for(Map.Entry<Long, Integer> entry : mergedItems.entrySet()) {
    		Long menuId = entry.getKey();
    		Integer quantity = entry.getValue();
    		
    		Menu menu = menuRepository.findById(menuId)
    				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 메뉴입니다."));
    		
    		OrderItems orderItem = OrderItems.createOrderItem(savedOrder, menu, quantity);
    		orderItemsRepository.save(orderItem);
    		totalPrice += menu.getPrice() * quantity;
    	}
    	
    	savedOrder.updateTotalPrice(totalPrice);
    	return savedOrder;
    	
    	
    }
    
    /**
     * 장바구니에 중복해서 담긴 동일한 메뉴를 하나로 병합 
     * */
    private Map<Long, Integer> mergeItems(List<CartItemRequest> items) {
    	Map<Long, Integer> mergedItems = new LinkedHashMap<>(); // 빈바구니
    	
    	for(CartItemRequest item : items) {
    		if(item.menuId() == null) {
    			throw new IllegalArgumentException("menuId는 필수입니다.");
    		}
    		
    		if(item.quantity() == null || item.quantity() <= 0) {
    			throw new IllegalArgumentException("수량은 1 이상이어야 합니다.");
    		}
    		
    		mergedItems.merge(item.menuId(), item.quantity(), Integer::sum); // Map 인터페이스의 메서드 
    	}
    	
    	return mergedItems;
    }
    
    /**
     * 주문 결제 내역 Order, OrderItem을 조회해서 상세 정보(DTO)를 제공 
     * */
    public IndividualOrderReceiptResponseDTO getIndividualReceipt(
            Long userId,
            String orderNo
            ) {
        Order order = orderRepository
                .findByOrderNoAndUser_UserIdAndGroupOrderIsNull(orderNo, userId)
                .orElseThrow(() -> new IllegalArgumentException("개별 주문 영수증을 찾을 수 없습니다."));

        List<OrderItems> orderItems = orderItemsRepository.findByOrder_OrderId(order.getOrderId());

        List<OrderReceiptItemDTO> items = orderItems.stream()
                .map(item -> new OrderReceiptItemDTO(
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getProduct().getPrice()
                        ))
                .toList();

        return new IndividualOrderReceiptResponseDTO(
                order.getOrderId(),
                order.getTotalPrice(),
                order.getStatus().name(),
                order.getCreatedAt(),
                items
                );
    }
    
    

}
