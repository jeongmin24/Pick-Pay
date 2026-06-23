package com.ssafy.pickpay.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
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
    private final FirebaseSyncService firebaseSyncService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public GroupOrder createSession(Long userId) {
        Optional<GroupOrder> existingGroupOrder =
                groupOrderRepository.findByHost_UserIdAndStatus(userId, GroupOrderStatus.OPEN);
        if (existingGroupOrder.isPresent()) {
            return existingGroupOrder.get();
        }

        User host = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        GroupOrder groupOrder = GroupOrder.createGroupOrder(host);
        GroupOrder savedOrder = groupOrderRepository.save(groupOrder);

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("group_orders/" + savedOrder.getGroupId());

        Map<String, Object> initialData = new HashMap<>();
        initialData.put("status", GroupOrderStatus.OPEN.name());
        initialData.put("hostId", userId);

        ref.setValueAsync(initialData);

        return savedOrder;
    }

    @Transactional(readOnly = true)
    public GroupJoinResponseDTO joinGroup(Long userId, String shareToken) {
        GroupOrder groupOrder = groupOrderRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid invite link."));

        GroupOrderStatus status = groupOrder.getStatus();

        if (status == GroupOrderStatus.PAID) {
            throw new IllegalStateException("This group order is already paid.");
        }

        if (status == GroupOrderStatus.LOCKED) {
            throw new IllegalStateException("This group order is already closed.");
        }

        boolean isHost = groupOrder.getHost().getUserId().equals(userId);

        return new GroupJoinResponseDTO(
                groupOrder.getGroupId(),
                groupOrder.getStatus().name(),
                isHost
        );
    }

    @Transactional
    public void closeSession(Long requestUserId, String groupId, GroupPayType payType) {
        GroupOrder groupOrder = groupOrderRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found."));

        validateCanCloseGroupOrder(groupOrder, requestUserId);

        List<FirebaseCartItemDTO> firebaseItems;
        try {
            firebaseItems = firebaseSyncService.getCartItems(groupId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while reading Firebase cart.", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new RuntimeException("Failed to read Firebase cart.", e);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error while reading Firebase cart.", e);
        }

        if (firebaseItems.isEmpty()) {
            throw new IllegalStateException("Cannot close an empty cart.");
        }

        Map<Long, List<FirebaseCartItemDTO>> itemsByUser = firebaseItems.stream()
                .collect(Collectors.groupingBy(FirebaseCartItemDTO::getUserId));

        List<Order> createdOrders = switch (payType) {
            case DUTCH -> createGroupOrdersByUser(groupId, itemsByUser);
            case HOST -> List.of(createGroupOrderForHost(requestUserId, groupId, firebaseItems));
        };

        groupOrder.closeAndSetPayType(payType);
        firebaseSyncService.updateFirebaseGroupStatus(groupId, payType);

        if (payType == GroupPayType.DUTCH) {
            eventPublisher.publishEvent(
                    GroupDutchPaymentRequestedEvent.from(groupId, createdOrders)
            );
        }
    }

    private List<Order> createGroupOrdersByUser(
            String groupId,
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
            String groupId,
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
            throw new AccessDeniedException("Only the host can close the group order.");
        }

        if (groupOrder.getStatus() == GroupOrderStatus.PAID) {
            throw new IllegalStateException("This group order is already paid.");
        }

        if (groupOrder.getStatus() == GroupOrderStatus.LOCKED) {
            throw new IllegalStateException("This group order is already closed.");
        }

        if (groupOrder.getStatus() != GroupOrderStatus.OPEN) {
            throw new IllegalStateException("This group order cannot be closed.");
        }
    }

    @Transactional(readOnly = true)
    public GroupOrderReceiptResponseDTO getReceipt(String groupId) {
        GroupOrder groupOrder = groupOrderRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found."));

        List<Order> orders = orderRepository.findByGroupOrder_GroupId(groupId);

        Long totalGroupPrice = 0L;
        List<UserReceiptDTO> userReceiptList = new ArrayList<>();

        for (Order order : orders) {
            totalGroupPrice += order.getTotalPrice();
            List<OrderItems> rawOrderItems = orderItemsRepository.findByOrder_OrderId(order.getOrderId());
            List<OrderReceiptItemDTO> itemDTOList = rawOrderItems.stream()
                    .map(item -> new OrderReceiptItemDTO(
                            item.getProduct().getName(),
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
