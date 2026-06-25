package com.ssafy.pickpay.service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
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
import com.ssafy.pickpay.dto.PickupRouletteResponseDTO;
import com.ssafy.pickpay.dto.PickupRouletteResponseDTO.PickupCandidateDTO;
import com.ssafy.pickpay.repository.GroupOrderRepository;
import com.ssafy.pickpay.repository.OrderItemsRepository;
import com.ssafy.pickpay.repository.OrderRepository;
import com.ssafy.pickpay.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GroupOrderService {

    private static final long PICKUP_ROULETTE_DURATION_MS = 3200L;

    private final SecureRandom secureRandom = new SecureRandom();

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
            GroupOrder groupOrder = existingGroupOrder.get();
            upsertFirebasePickupCandidate(groupOrder.getGroupId(), userId);
            return groupOrder;
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
        initialData.put("pickupRoulette", buildInitialPickupRouletteData(host));

        ref.setValueAsync(initialData);

        return savedOrder;
    }

    @Transactional
    public GroupJoinResponseDTO joinGroup(Long userId, String shareToken) {
        GroupOrder groupOrder = groupOrderRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid invite link."));

        GroupOrderStatus status = groupOrder.getStatus();

        if (status == GroupOrderStatus.PAID || status == GroupOrderStatus.PAYMENT_FAILED) {
            throw new IllegalStateException("This group order is already paid.");
        }

        if (status == GroupOrderStatus.LOCKED || status == GroupOrderStatus.PAYMENT_PENDING) {
            throw new IllegalStateException("This group order is already closed.");
        }

        boolean isHost = groupOrder.getHost().getUserId().equals(userId);
        upsertFirebasePickupCandidate(groupOrder.getGroupId(), userId);

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

    @Transactional
    public PickupRouletteResponseDTO selectPickupWinner(Long requestUserId, String groupId) {
        GroupOrder groupOrder = groupOrderRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹을 찾을수 없습니다."));

        validateHost(groupOrder, requestUserId);

        List<PickupCandidateDTO> candidates = getPickupCandidates(groupId);
        if (candidates.isEmpty()) {
            throw new IllegalStateException("빈 장바구니에서 픽업 유저를 뽑을 수 없습니다.");
        }

        if (groupOrder.getStatus() != GroupOrderStatus.OPEN) {
            throw new IllegalStateException("OPEN 상태 그룹에서만 픽업 유저 선정이 가능합니다.");
        }

        int winnerIndex = secureRandom.nextInt(candidates.size());
        PickupCandidateDTO selectedCandidate = candidates.get(winnerIndex);
        User winner = userRepository.findById(selectedCandidate.userId())
                .orElseThrow(() -> new IllegalArgumentException("픽업 유저를 찾을 수 없습니다."));

        String roundId = UUID.randomUUID().toString();
        long startedAt = System.currentTimeMillis();

        groupOrder.setPickupUser(winner);
        updateFirebasePickupRoulette(groupId, roundId, startedAt, winner, winnerIndex, candidates);

        return new PickupRouletteResponseDTO(
                groupId,
                roundId,
                "SPINNING",
                startedAt,
                PICKUP_ROULETTE_DURATION_MS,
                winner.getUserId(),
                winner.getNickname(),
                winnerIndex,
                false,
                false,
                candidates
        );
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

    private List<PickupCandidateDTO> getPickupCandidates(String groupId) {
        List<PickupCandidateDTO> rouletteCandidates = getPickupCandidatesFromRoulette(groupId);
        if (!rouletteCandidates.isEmpty()) {
            return rouletteCandidates;
        }

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

        List<Long> candidateUserIds = firebaseItems.stream()
                .map(FirebaseCartItemDTO::getUserId)
                .filter(userId -> userId != null && userId > 0)
                .collect(Collectors.toCollection(ArrayList::new));

        List<Long> distinctUserIds = new ArrayList<>(new LinkedHashSet<>(candidateUserIds));
        if (distinctUserIds.isEmpty()) {
            return List.of();
        }

        Map<Long, User> usersById = new HashMap<>();
        userRepository.findAllById(distinctUserIds)
                .forEach(user -> usersById.put(user.getUserId(), user));

        return distinctUserIds.stream()
                .map(userId -> {
                    User user = usersById.get(userId);
                    if (user == null) {
                        throw new IllegalArgumentException("Candidate user not found.");
                    }
                    return new PickupCandidateDTO(user.getUserId(), user.getNickname());
                })
                .toList();
    }

    private List<PickupCandidateDTO> getPickupCandidatesFromRoulette(String groupId) {
        CompletableFuture<List<PickupCandidateDTO>> future = new CompletableFuture<>();
        DatabaseReference candidatesRef = FirebaseDatabase.getInstance()
                .getReference("group_orders/" + groupId + "/pickupRoulette/candidates");

        candidatesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<PickupCandidateDTO> candidates = new ArrayList<>();
                for (DataSnapshot candidateSnapshot : snapshot.getChildren()) {
                    Long userId = candidateSnapshot.child("userId").getValue(Long.class);
                    String nickname = candidateSnapshot.child("nickname").getValue(String.class);
                    if (userId != null && userId > 0) {
                        candidates.add(new PickupCandidateDTO(userId, nickname));
                    }
                }
                future.complete(candidates.stream()
                        .collect(Collectors.collectingAndThen(
                                Collectors.toMap(
                                        PickupCandidateDTO::userId,
                                        candidate -> candidate,
                                        (first, ignored) -> first,
                                        java.util.LinkedHashMap::new
                                ),
                                map -> new ArrayList<>(map.values())
                        )));
            }

            @Override
            public void onCancelled(DatabaseError error) {
                future.completeExceptionally(error.toException());
            }
        });

        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while reading pickup candidates.", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new RuntimeException("Failed to read pickup candidates.", e);
        }
    }

    private Map<String, Object> buildInitialPickupRouletteData(User host) {
        Map<String, Object> pickupRoulette = new HashMap<>();
        Map<String, Object> candidates = new HashMap<>();
        candidates.put(String.valueOf(host.getUserId()), buildPickupCandidateMap(host));

        pickupRoulette.put("status", "READY");
        pickupRoulette.put("durationMs", PICKUP_ROULETTE_DURATION_MS);
        pickupRoulette.put("chatPushed", false);
        pickupRoulette.put("candidates", candidates);
        return pickupRoulette;
    }

    private void upsertFirebasePickupCandidate(String groupId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        DatabaseReference candidateRef = FirebaseDatabase.getInstance()
                .getReference("group_orders/" + groupId + "/pickupRoulette/candidates/" + userId);

        try {
            candidateRef.setValueAsync(buildPickupCandidateMap(user)).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while updating pickup candidate.", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to update pickup candidate.", e);
        }
    }

    private Map<String, Object> buildPickupCandidateMap(User user) {
        Map<String, Object> candidate = new HashMap<>();
        candidate.put("userId", user.getUserId());
        candidate.put("nickname", user.getNickname());
        return candidate;
    }

    private void updateFirebasePickupRoulette(
            String groupId,
            String roundId,
            long startedAt,
            User winner,
            int winnerIndex,
            List<PickupCandidateDTO> candidates
    ) {
        DatabaseReference rouletteRef = FirebaseDatabase.getInstance()
                .getReference("group_orders/" + groupId + "/pickupRoulette");

        Map<String, Object> updates = new HashMap<>();
        updates.put("roundId", roundId);
        updates.put("status", "SPINNING");
        updates.put("startedAt", startedAt);
        updates.put("durationMs", PICKUP_ROULETTE_DURATION_MS);
        updates.put("winnerUserId", winner.getUserId());
        updates.put("winnerNickname", winner.getNickname());
        updates.put("winnerIndex", winnerIndex);
        updates.put("selectedAt", startedAt);
        updates.put("chatPushed", false);
        updates.put("candidates", candidates.stream()
                .map(candidate -> {
                    Map<String, Object> candidateMap = new HashMap<>();
                    candidateMap.put("userId", candidate.userId());
                    candidateMap.put("nickname", candidate.nickname());
                    return candidateMap;
                })
                .toList());

        try {
            rouletteRef.setValueAsync(updates).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while updating pickup roulette.", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to update pickup roulette.", e);
        }
    }

    private void validateCanCloseGroupOrder(GroupOrder groupOrder, Long requestUserId) {
        validateHost(groupOrder, requestUserId);

        if (groupOrder.getStatus() == GroupOrderStatus.PAID) {
            throw new IllegalStateException("This group order is already paid.");
        }

        if (groupOrder.getStatus() == GroupOrderStatus.LOCKED) {
            throw new IllegalStateException("This group order is already closed.");
        }

        if (groupOrder.getStatus() == GroupOrderStatus.PAYMENT_PENDING) {
            throw new IllegalStateException("This group order is already waiting for payment.");
        }

        if (groupOrder.getStatus() != GroupOrderStatus.OPEN) {
            throw new IllegalStateException("This group order cannot be closed.");
        }
    }

    private void validateHost(GroupOrder groupOrder, Long requestUserId) {
        if (!groupOrder.getHost().getUserId().equals(requestUserId)) {
            throw new AccessDeniedException("Only the host can manage this group order.");
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
                    .displayOrderNo(order.getDisplayOrderNo())
                    .nickname(order.getUser().getNickname())
                    .orderStatus(order.getStatus().name())
                    .userTotalPrice(order.getTotalPrice())
                    .items(itemDTOList)
                    .build();

            userReceiptList.add(userReceiptDTO);
        }

        return GroupOrderReceiptResponseDTO.builder()
                .groupId(groupOrder.getGroupId())
                .payType(groupOrder.getPayType() == null ? null : groupOrder.getPayType().name())
                .groupStatus(groupOrder.getStatus().name())
                .totalGroupPrice(totalGroupPrice)
                .userReceipts(userReceiptList)
                .build();
    }
}
