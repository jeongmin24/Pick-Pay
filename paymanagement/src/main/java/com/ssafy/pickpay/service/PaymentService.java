package com.ssafy.pickpay.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.ssafy.pickpay.common.ErrorCode;
import com.ssafy.pickpay.common.GroupOrderStatus;
import com.ssafy.pickpay.common.GroupPayType;
import com.ssafy.pickpay.common.OrderStatus;
import com.ssafy.pickpay.common.PaymentStatus;
import com.ssafy.pickpay.domain.GroupOrder;
import com.ssafy.pickpay.domain.Menu;
import com.ssafy.pickpay.domain.Order;
import com.ssafy.pickpay.domain.OrderItems;
import com.ssafy.pickpay.domain.Payment;
import com.ssafy.pickpay.dto.PaymentCompleteRequestDTO;
import com.ssafy.pickpay.dto.PaymentCompleteResponseDTO;
import com.ssafy.pickpay.dto.PaymentFailRequestDTO;
import com.ssafy.pickpay.dto.PgConfirmResponse;
import com.ssafy.pickpay.exception.BusinessException;
import com.ssafy.pickpay.infra.pg.PgPaymentClient;
import com.ssafy.pickpay.repository.MenuRepository;
import com.ssafy.pickpay.repository.OrderItemsRepository;
import com.ssafy.pickpay.repository.OrderRepository;
import com.ssafy.pickpay.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {
    private static final String PG_APPROVED_STATUS = "DONE";
    private static final String COMPENSATION_CANCEL_REASON = "Dutch group payment failed";
    private static final Set<PaymentStatus> CANCELABLE_PAYMENT_STATUSES = Set.of(
            PaymentStatus.APPROVED_PENDING_GROUP,
            PaymentStatus.APPROVED
    );

    private final OrderRepository orderRepository;
    private final OrderItemsRepository orderItemsRepository;
    private final MenuRepository menuRepository;
    private final PaymentRepository paymentRepository;
    private final PgPaymentClient pgPaymentClient;
    private final FirebaseSyncService firebaseSyncService;
    private final TransactionTemplate transactionTemplate;

    @Value("${payment.expiration-minutes:30}")
    private long paymentExpirationMinutes;

    public PaymentCompleteResponseDTO completePayment(
            Long userId,
            PaymentCompleteRequestDTO request
    ) {
        Order order = orderRepository.findByOrderNoWithUserAndGroupOrder(request.orderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        validateOrderOwner(order, userId);

        log.info(
                "Payment order matched orderNo={}, dbAmount={}, status={}, requestAmount={}",
                order.getOrderNo(),
                order.getTotalPrice(),
                order.getStatus(),
                request.amount()
        );

        if (order.isPaid()) {
            return createAlreadyPaidResponse(order);
        }

        if (isDutchPaymentApproved(order)) {
            validateAmount(order, request.amount());
            return createDutchPaymentApprovedResponse(order);
        }

        try {
            validateOrderPayable(order);
            validatePaymentNotExpired(order);
            validateAmount(order, request.amount());
        } catch (BusinessException exception) {
            if (isDutchGroupOrder(order) && exception.getErrorCode() == ErrorCode.PAYMENT_EXPIRED) {
                compensateDutchGroupAfterDefinitiveFailure(
                        request.orderId(),
                        null,
                        "더치페이 결제 시간이 만료되었습니다."
                );
            }
            throw exception;
        }

        // PG사로 요청 (I/O) 
        PgConfirmResponse pgResponse;
        try {
            pgResponse = pgPaymentClient.confirmPayment(
                    request.paymentKey(),
                    request.orderId(),
                    request.amount()
            );
        } catch (BusinessException exception) {
            if (isDutchGroupOrder(order) && exception.getErrorCode() == ErrorCode.PG_CONFIRM_REJECTED) {
                compensateDutchGroupAfterDefinitiveFailure(
                        request.orderId(),
                        null,
                        "PG사에 결제가 거절되었습니다."
                );
            }
            throw exception;
        }

        // PG사 응답값 검증 
        try {
            validatePgResponse(order, request.paymentKey(), pgResponse);
        } catch (BusinessException exception) {
            if (shouldCancelConfirmedPayment(request, pgResponse)) {
                cancelApprovedPaymentAfterDbFailure(request, exception);
            }
            throw exception;
        }

        log.info(
                "Payment Toss confirm response orderId={}, totalAmount={}, status={}",
                pgResponse.orderId(),
                pgResponse.totalAmount(),
                pgResponse.status()
        );

        // PG 승인 후 DB작업을 트랜잭션 안에서 수행 (transactionTemplate) 
        try {
            return transactionTemplate.execute(status ->
                    completePaymentInTransaction(userId, request, pgResponse)
            );
        } catch (RuntimeException dbException) {
            if (shouldFailDutchGroupAfterApprovedProcessing(order, dbException)) {
                compensateDutchGroupAfterApprovedFailure( // 더치페이면 그룹에 해당하는 order 모두 취소
                        request.orderId(),
                        request.paymentKey(),
                        dbException
                );
            } else { // 더치페이가 아니면 내 결제 1건만 취소 
                cancelApprovedPaymentAfterDbFailure(request, dbException);
            }

            // 에러 응답 리턴 
            if (dbException instanceof BusinessException businessException) { 
                throw businessException;
            }
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, dbException);
        }
    }

    public PaymentCompleteResponseDTO failPayment(
            Long userId,
            PaymentFailRequestDTO request
    ) {
        Order order = orderRepository.findByOrderNoWithUserAndGroupOrder(request.orderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        validateOrderOwner(order, userId);

        if (order.isPaid()
                || (order.getGroupOrder() != null
                && order.getGroupOrder().getStatus() == GroupOrderStatus.PAID)) {
            return createAlreadyPaidResponse(order);
        }

        if (order.getGroupOrder() != null
                && order.getGroupOrder().getStatus() == GroupOrderStatus.PAYMENT_FAILED) {
            return createPaymentResponse(order, "Group payment already failed.");
        }

        if (isDutchGroupOrder(order)) {
            compensateDutchGroupAfterDefinitiveFailure(
                    request.orderId(),
                    null,
                    request.reason()
            );

            Order failedOrder = orderRepository.findByOrderNoWithUserAndGroupOrder(request.orderId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
            if (failedOrder.isPaid()
                    || failedOrder.getGroupOrder().getStatus() == GroupOrderStatus.PAID) {
                return createAlreadyPaidResponse(failedOrder);
            }
            return createPaymentResponse(
                    failedOrder,
                    failedOrder.getGroupOrder().getStatus() == GroupOrderStatus.PAYMENT_FAILED
                            ? "Group payment failed."
                            : "Payment failure was ignored because the group is still active."
            );
        }

        return transactionTemplate.execute(status -> {
            Order lockedOrder = orderRepository.findByOrderNoForUpdate(request.orderId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
            validateOrderOwner(lockedOrder, userId);
            if (lockedOrder.isPaid()) {
                return createAlreadyPaidResponse(lockedOrder);
            }
            lockedOrder.markPaymentFailed();
            return createPaymentResponse(
                    lockedOrder,
                    "Payment failed."
            );
        });
    }

    private PaymentCompleteResponseDTO completePaymentInTransaction(
            Long userId,
            PaymentCompleteRequestDTO request,
            PgConfirmResponse pgResponse
    ) {
    	// orderNo로 비관적락(for update)을 걸고 주문을 가져옴 
        Order lockedOrder = orderRepository.findByOrderNoForUpdate(request.orderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        validateOrderOwner(lockedOrder, userId);
        if (lockedOrder.isPaid()) {
            return createAlreadyPaidResponse(lockedOrder);
        }

        if (isDutchPaymentApproved(lockedOrder)) {
            validateAmount(lockedOrder, request.amount());
            return createDutchPaymentApprovedResponse(lockedOrder);
        }

        validateOrderPayable(lockedOrder);
        validatePaymentNotExpired(lockedOrder);
        validateAmount(lockedOrder, request.amount());
        validatePgResponse(lockedOrder, request.paymentKey(), pgResponse);

        // Payment가 존재하지 않으면 새로 생성 
        Payment payment = paymentRepository.findByPaymentKey(request.paymentKey())
                .orElseGet(() -> Payment.approving(
                        lockedOrder,
                        request.paymentKey(),
                        pgResponse.totalAmount()
                ));

        if (!Objects.equals(payment.getOrder().getOrderId(), lockedOrder.getOrderId())) {
            throw new BusinessException(ErrorCode.PAYMENT_CONFLICT);
        }

        if (!Objects.equals(payment.getAmount(), pgResponse.totalAmount())) {
            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        // 1. 더치페이 분기 
        if (isDutchGroupOrder(lockedOrder)) {
            return completeDutchPaymentInTransaction(lockedOrder, payment);
        }

        // 2. 일반 결제 또는 호스트 단독 결제 분기 
        decreaseMenuStock(lockedOrder);
        lockedOrder.markPaid();
        payment.approve();
        paymentRepository.save(payment);

        // 3. 호스트 결제인 경우 그룹 주문 전체를 PAID로 변경 
        updateGroupOrderIfNeeded(lockedOrder);

        return createPaymentResponse(
                lockedOrder,
                "Payment completed."
        );
    }

    private PaymentCompleteResponseDTO completeDutchPaymentInTransaction(
            Order lockedOrder,
            Payment payment
    ) {
    	// 현재 결제한 유저의 주문과 결제 상태를 대기, 중간 상태로 변경
        lockedOrder.markPaymentApproved(); // OrderStatus.PAYMENT_APPROVED
        payment.approvePendingGroup(); // PaymentStatus.APPROVED_PENDING_GROUP
        paymentRepository.save(payment);

        // groupId로 같은 그룹 주문을 찾아서 락을 걸고(for update) 가져옴
        GroupOrder groupOrder = lockedOrder.getGroupOrder();
        List<Order> groupOrders = orderRepository.findByGroupOrderGroupIdForUpdate(
                groupOrder.getGroupId()
        );

        // 누군가 실패했거나 취소한 주문이 있다면 전체 취소를 유도 
        boolean hasFailedOrder = groupOrders.stream()
                .anyMatch(order -> order.getStatus() == OrderStatus.PAYMENT_FAILED
                        || order.getStatus() == OrderStatus.CANCELLED);

        if (hasFailedOrder) {
            throw new BusinessException(ErrorCode.PAYMENT_CONFLICT);
        }

        // 모든 멤버가 결제 승인(PAYMEN_APPROVED) 상태가 되었는지 확인 
        boolean allApproved = groupOrders.stream()
                .allMatch(order -> order.getStatus() == OrderStatus.PAYMENT_APPROVED);

        // 아직 결제 안한 멤버가 남아있다면 확정을 미루고 대기 메시지 리턴 
        if (!allApproved) {
            return createPaymentResponse(
                    lockedOrder,
                    "다른 사람들 결제가 끝날때까지 기다려주세요."
            );
        }

        // 모두 승인 된 경우 일괄 확정 처리 !! 여기서 메뉴 재고 확인해서 결제가 안되게 막아야함 
        decreaseMenuStockForDutchOrders(groupOrders);

        groupOrders.forEach(Order::markPaid); // 모든 멤버의 주문 상태를 PAID로 변경 
        paymentRepository.findByGroupId(groupOrder.getGroupId()) // 모든 결제 상태를 APPROVED로 변경 
                .forEach(Payment::approve);
        groupOrder.markPaid(); // 그룹 주문 자체를 최종 PAID로 변경 
        updateFirebaseGroupPaymentStatusQuietly(groupOrder.getGroupId(), GroupOrderStatus.PAID); // 파이어베이스 동기화 

        return createPaymentResponse(
                lockedOrder,
                "모든 멤버 결제가 완료됐습니다."
        );
    }

    private void compensateDutchGroupAfterApprovedFailure(
            String orderNo,
            String currentPaymentKey,
            RuntimeException dbException
    ) {
        DutchCompensationTarget target = compensateDutchGroupAfterDefinitiveFailure(
                orderNo,
                currentPaymentKey,
                COMPENSATION_CANCEL_REASON
        );

        if (target.cancelPaymentKeys().isEmpty()) {
            return;
        }

        log.warn(
                "Dutch group compensation completed groupId={}, canceledPayments={}",
                target.groupId(),
                target.cancelPaymentKeys().size()
        );

        if (dbException instanceof BusinessException businessException) {
            throw businessException;
        }
    }

    private DutchCompensationTarget compensateDutchGroupAfterDefinitiveFailure(
            String orderNo,
            String currentPaymentKey,
            String reason
    ) {
        DutchCompensationTarget target = transactionTemplate.execute(status ->
                markDutchGroupFailedAndCollectCancelTargets(orderNo, currentPaymentKey)
        );

        if (target == null || target.cancelPaymentKeys().isEmpty()) {
            DutchCompensationTarget safeTarget = target == null
                    ? new DutchCompensationTarget(null, List.of())
                    : target;
            updateFirebaseGroupPaymentStatusQuietly(
                    safeTarget.groupId(),
                    GroupOrderStatus.PAYMENT_FAILED
            );
            return safeTarget;
        }

        List<String> canceledPaymentKeys = cancelApprovedPayments(target.cancelPaymentKeys(), reason);

        transactionTemplate.executeWithoutResult(status ->
                markPaymentsCanceled(canceledPaymentKeys, reason)
        );

        updateFirebaseGroupPaymentStatusQuietly(target.groupId(), GroupOrderStatus.PAYMENT_FAILED);

        return new DutchCompensationTarget(target.groupId(), canceledPaymentKeys);
    }

    private DutchCompensationTarget markDutchGroupFailedAndCollectCancelTargets(
            String orderNo,
            String currentPaymentKey
    ) {
        Order failedOrder = orderRepository.findByOrderNoForUpdate(orderNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (!isDutchGroupOrder(failedOrder)) {
            return new DutchCompensationTarget(null, List.of());
        }

        GroupOrder groupOrder = failedOrder.getGroupOrder();
        List<Order> groupOrders = orderRepository.findByGroupOrderGroupIdForUpdate(
                groupOrder.getGroupId()
        );

        if (groupOrder.getStatus() == GroupOrderStatus.PAID) {
            return new DutchCompensationTarget(null, List.of());
        }

        groupOrders.forEach(Order::markPaymentFailed);
        groupOrder.markPaymentFailed();

        LinkedHashSet<String> paymentKeys = paymentRepository.findByGroupId(groupOrder.getGroupId())
                .stream()
                .filter(payment -> CANCELABLE_PAYMENT_STATUSES.contains(payment.getStatus()))
                .map(Payment::getPaymentKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (currentPaymentKey != null && !currentPaymentKey.isBlank()) {
            paymentKeys.add(currentPaymentKey);
        }

        return new DutchCompensationTarget(
                groupOrder.getGroupId(),
                new ArrayList<>(paymentKeys)
        );
    }

    private List<String> cancelApprovedPayments(List<String> paymentKeys, String reason) {
        List<String> canceledPaymentKeys = new ArrayList<>();

        for (String paymentKey : paymentKeys) {
            String idempotencyKey = "dutch-group-cancel-"
                    + Integer.toUnsignedString(paymentKey.hashCode());

            pgPaymentClient.cancelPayment(
                    paymentKey,
                    reason,
                    idempotencyKey
            );
            canceledPaymentKeys.add(paymentKey);
        }

        return canceledPaymentKeys;
    }

    private void markPaymentsCanceled(List<String> paymentKeys, String reason) {
        for (String paymentKey : paymentKeys) {
            paymentRepository.findByPaymentKey(paymentKey)
                    .ifPresent(payment -> payment.cancel(reason));
        }
    }

    private void cancelApprovedPaymentAfterDbFailure(
            PaymentCompleteRequestDTO request,
            RuntimeException dbException
    ) {
        String idempotencyKey = "payment-db-failure-"
                + request.orderId()
                + "-"
                + Integer.toUnsignedString(request.paymentKey().hashCode());

        try {
            pgPaymentClient.cancelPayment(
                    request.paymentKey(),
                    "Payment canceled after order processing failure",
                    idempotencyKey
            );
        } catch (RuntimeException cancelException) {
            dbException.addSuppressed(cancelException);
            log.error(
                    "Payment compensation cancel failed orderId={}, paymentKey={}",
                    request.orderId(),
                    maskPaymentKey(request.paymentKey()),
                    cancelException
            );
            throw new BusinessException(ErrorCode.PG_CANCEL_FAILED, dbException);
        }
    }

    private void validateOrderOwner(Order order, Long loginUserId) {
        if (!Objects.equals(order.getUser().getUserId(), loginUserId)) {
            throw new BusinessException(ErrorCode.ORDER_FORBIDDEN);
        }
    }

    private void validateOrderPayable(Order order) {
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException(ErrorCode.PAYMENT_CONFLICT);
        }
    }

    private void validatePaymentNotExpired(Order order) {
        if (order.getCreatedAt() == null) {
            return;
        }

        LocalDateTime expiredAt = order.getCreatedAt().plusMinutes(paymentExpirationMinutes);
        if (LocalDateTime.now().isAfter(expiredAt)) {
            throw new BusinessException(ErrorCode.PAYMENT_EXPIRED);
        }
    }

    private void validateAmount(Order order, Long requestAmount) {
        if (!Objects.equals(order.getTotalPrice(), requestAmount)) {
            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }
    }

    private void validatePgResponse(Order order, String paymentKey, PgConfirmResponse pgResponse) {
        if (pgResponse == null) {
            throw new BusinessException(ErrorCode.PG_CONFIRM_FAILED);
        }

        if (!Objects.equals(paymentKey, pgResponse.paymentKey())) {
            throw new BusinessException(ErrorCode.PG_CONFIRM_REJECTED);
        }

        if (!Objects.equals(order.getOrderNo(), pgResponse.orderId())) {
            throw new BusinessException(ErrorCode.PG_CONFIRM_REJECTED);
        }

        if (!Objects.equals(order.getTotalPrice(), pgResponse.totalAmount())) {
            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        if (!PG_APPROVED_STATUS.equals(pgResponse.status())) {
            throw new BusinessException(ErrorCode.PG_CONFIRM_REJECTED);
        }
    }

    private void decreaseMenuStock(Order order) {
        decreaseMenuStockForOrders(List.of(order));
    }

    private void decreaseMenuStockForDutchOrders(List<Order> orders) {
        try {
            decreaseMenuStockForOrders(orders);
        } catch (BusinessException exception) {
//            if (exception.getErrorCode() != ErrorCode.OUT_OF_STOCK) {
//                throw exception;
//            }

            log.warn(
                    "Dutch group stock decrease skipped because stock data is unavailable or insufficient. orderIds={}",
                    orders.stream()
                            .map(Order::getOrderId)
                            .toList(),
                    exception
            );
            throw exception;
        }
    }

    private void decreaseMenuStockForOrders(List<Order> orders) {
        List<OrderItems> allOrderItems = orders.stream()
                .flatMap(order -> orderItemsRepository
                        .findAllByOrderIdWithProduct(order.getOrderId())
                        .stream())
                .toList();

        if (allOrderItems.isEmpty()) {
            throw new BusinessException(ErrorCode.PAYMENT_CONFLICT);
        }

        Map<Long, Integer> quantitiesByMenuId = allOrderItems.stream()
                .collect(Collectors.groupingBy(
                        orderItem -> orderItem.getProduct().getMenuId(),
                        Collectors.summingInt(OrderItems::getQuantity)
                ));

        List<Long> menuIds = quantitiesByMenuId.keySet()
                .stream()
                .sorted()
                .toList();

        List<Menu> lockedMenus = menuRepository.findAllByMenuIdsForUpdate(menuIds);

        Map<Long, Menu> menuMap = lockedMenus.stream()
                .collect(Collectors.toMap(Menu::getMenuId, menu -> menu));

        for (Long menuId : menuIds) {
            Menu lockedMenu = menuMap.get(menuId);

            if (lockedMenu == null) {
                throw new BusinessException(ErrorCode.PAYMENT_CONFLICT);
            }

            lockedMenu.decreaseStock(quantitiesByMenuId.get(menuId));
        }
    }

    private void updateGroupOrderIfNeeded(Order order) {
        GroupOrder groupOrder = order.getGroupOrder();

        if (groupOrder == null) {
            return;
        }

        boolean hasUnpaidOrder = orderRepository.existsByGroupOrder_GroupIdAndStatusNot(
                groupOrder.getGroupId(),
                OrderStatus.PAID
        );

        if (!hasUnpaidOrder) {
            groupOrder.markPaid();
        }
    }

    private boolean isDutchGroupOrder(Order order) {
        return order.getGroupOrder() != null
                && order.getGroupOrder().getPayType() == GroupPayType.DUTCH;
    }

    private boolean isDutchPaymentApproved(Order order) {
        return isDutchGroupOrder(order)
                && order.getStatus() == OrderStatus.PAYMENT_APPROVED;
    }

    private boolean shouldFailDutchGroupAfterApprovedProcessing(
            Order order,
            RuntimeException exception
    ) {
//        return false;
    	if(!isDutchGroupOrder(order)) {
    		return false;
    	}
    	
    	return exception instanceof BusinessException businessException
    			&& businessException.getErrorCode() == ErrorCode.OUT_OF_STOCK;
    }

    private boolean shouldCancelConfirmedPayment(
            PaymentCompleteRequestDTO request,
            PgConfirmResponse pgResponse
    ) {
        return pgResponse != null
                && Objects.equals(request.paymentKey(), pgResponse.paymentKey())
                && Objects.equals(request.orderId(), pgResponse.orderId())
                && PG_APPROVED_STATUS.equals(pgResponse.status());
    }

    private void updateFirebaseGroupPaymentStatusQuietly(
            String groupId,
            GroupOrderStatus status
    ) {
        if (groupId == null) {
            return;
        }

        try {
            firebaseSyncService.updateFirebaseGroupPaymentStatus(groupId, status);
        } catch (RuntimeException exception) {
            log.warn(
                    "Firebase group payment status sync failed groupId={}, status={}",
                    groupId,
                    status,
                    exception
            );
        }
    }

    private PaymentCompleteResponseDTO createPaymentResponse(Order order, String message) {
        GroupOrder groupOrder = order.getGroupOrder();

        return new PaymentCompleteResponseDTO(
                order.getOrderNo(),
                order.getTotalPrice(),
                order.getStatus().name(),
                groupOrder == null ? null : groupOrder.getGroupId(),
                groupOrder == null ? null : groupOrder.getStatus().name(),
                message
        );
    }

    private PaymentCompleteResponseDTO createAlreadyPaidResponse(Order order) {
        return createPaymentResponse(order, "Order is already paid.");
    }

    private PaymentCompleteResponseDTO createDutchPaymentApprovedResponse(Order order) {
        return createPaymentResponse(
                order,
                "Payment already approved. Waiting for other members."
        );
    }

    private String maskPaymentKey(String paymentKey) {
        if (paymentKey == null || paymentKey.length() <= 12) {
            return "***";
        }
        return paymentKey.substring(0, 6) + "..." + paymentKey.substring(paymentKey.length() - 4);
    }

    private record DutchCompensationTarget(
            String groupId,
            List<String> cancelPaymentKeys
    ) {
    }
}
