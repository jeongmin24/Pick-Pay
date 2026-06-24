package com.ssafy.pickpay.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.ssafy.pickpay.common.OrderStatus;
import com.ssafy.pickpay.domain.GroupOrder;
import com.ssafy.pickpay.domain.Menu;
import com.ssafy.pickpay.domain.Order;
import com.ssafy.pickpay.domain.OrderItems;
import com.ssafy.pickpay.domain.Payment;
import com.ssafy.pickpay.dto.PaymentCompleteRequestDTO;
import com.ssafy.pickpay.dto.PaymentCompleteResponseDTO;
import com.ssafy.pickpay.dto.PgConfirmResponse;
import com.ssafy.pickpay.infra.pg.PgPaymentClient;
import com.ssafy.pickpay.repository.MenuRepository;
import com.ssafy.pickpay.repository.OrderItemsRepository;
import com.ssafy.pickpay.repository.OrderRepository;
import com.ssafy.pickpay.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service @RequiredArgsConstructor
public class PaymentService {
    private static final String PG_APPROVED_STATUS = "DONE";
    private static final String COMPENSATION_CANCEL_REASON = "주문 처리 실패로 인한 자동 결제 취소";
	
	private final OrderRepository orderRepository;
	private final OrderItemsRepository orderItemsRepository;
	private final MenuRepository menuRepository;
    private final PaymentRepository paymentRepository;
	private final PgPaymentClient pgPaymentClient;
    private final TransactionTemplate transactionTemplate;
	
    public PaymentCompleteResponseDTO completePayment(
            Long userId,
            PaymentCompleteRequestDTO request
    ) {
        Order order = orderRepository.findByOrderNoAndUser_UserId(request.orderId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

        log.info(
                "Payment order matched orderNo={}, dbAmount={}, status={}, requestAmount={}",
                order.getOrderNo(),
                order.getTotalPrice(),
                order.getStatus(),
                request.amount()
        );

        validateAmount(order, request.amount());

        if (order.isPaid()) {
            return new PaymentCompleteResponseDTO(
                    order.getOrderNo(),
                    order.getTotalPrice(),
                    order.getStatus().name(),
                    "이미 결제 완료된 주문입니다."
            );
        }

        // PG사 결제 승인 요청 
        PgConfirmResponse pgResponse = pgPaymentClient.confirmPayment(
                request.paymentKey(),
                request.orderId(),
                request.amount()
        );

        log.info(
                "Payment Toss confirm response orderId={}, totalAmount={}, status={}",
                pgResponse.orderId(),
                pgResponse.totalAmount(),
                pgResponse.status()
        );

        // PG사 결제 승인 성공 후 응답 검증 
        validatePgResponse(order, request.paymentKey(), pgResponse);

        // DB처리 트랜잭션 분리 
        try {
            return transactionTemplate.execute(status ->
                    completePaymentInTransaction(userId, request, pgResponse)
            );
        } catch (RuntimeException dbException) { // 결제 승인에 성공했는데 DB작업에서 예외가 나는 경우 -> Toss 취소 API 호출 
            cancelApprovedPaymentAfterDbFailure(request, dbException);
            throw new IllegalStateException("결제 승인 후 주문 처리에 실패하여 결제를 취소했습니다.", dbException);
        }
    }

    private PaymentCompleteResponseDTO completePaymentInTransaction(
            Long userId,
            PaymentCompleteRequestDTO request,
            PgConfirmResponse pgResponse
    ) {
        Order lockedOrder = orderRepository.findByOrderNoForUpdate(request.orderId())
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

        validateOrderOwner(lockedOrder, userId);
        validateAmount(lockedOrder, request.amount());
        validatePgResponse(lockedOrder, request.paymentKey(), pgResponse);

        if (lockedOrder.isPaid()) {
            return new PaymentCompleteResponseDTO(
                    lockedOrder.getOrderNo(),
                    lockedOrder.getTotalPrice(),
                    lockedOrder.getStatus().name(),
                    "이미 결제 완료된 주문입니다."
            );
        }

        Payment payment = paymentRepository.findByPaymentKey(request.paymentKey())
                .orElseGet(() -> Payment.approving(
                        lockedOrder,
                        request.paymentKey(),
                        pgResponse.totalAmount()
                ));

        if (!Objects.equals(payment.getOrder().getOrderId(), lockedOrder.getOrderId())) {
            throw new IllegalArgumentException("결제 키가 다른 주문에 이미 사용되었습니다.");
        }

        if (!Objects.equals(payment.getAmount(), pgResponse.totalAmount())) {
            throw new IllegalArgumentException("저장된 결제 금액과 PG 승인 금액이 일치하지 않습니다.");
        }

        decreaseMenuStock(lockedOrder);

        lockedOrder.markPaid();
        payment.approve();
        paymentRepository.save(payment);

        updateGroupOrderIfNeeded(lockedOrder);

        return new PaymentCompleteResponseDTO(
                lockedOrder.getOrderNo(),
                lockedOrder.getTotalPrice(),
                lockedOrder.getStatus().name(),
                "결제가 완료되었습니다."
        );
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
        	// cancelAmount 안넣으면 전액취소
            pgPaymentClient.cancelPayment(
                    request.paymentKey(),
                    COMPENSATION_CANCEL_REASON,
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
            throw new IllegalStateException(
                    "결제는 승인되었지만 주문 처리와 자동 취소가 모두 실패했습니다. 관리자 확인이 필요합니다.",
                    dbException
            );
        }
    }
	
	private void validateOrderOwner(Order order, Long loginUserId) {
        if (!Objects.equals(order.getUser().getUserId(), loginUserId)) {
            throw new IllegalArgumentException("본인의 주문만 결제할 수 있습니다.");
        }
    }

    private void validateAmount(Order order, Long requestAmount) {
        if (!Objects.equals(order.getTotalPrice(), requestAmount)) {
            throw new IllegalArgumentException("주문 금액이 일치하지 않습니다.");
        }
    }

    private void validatePgResponse(Order order, String paymentKey, PgConfirmResponse pgResponse) {
        if (pgResponse == null) {
            throw new IllegalArgumentException("PG 응답이 비어 있습니다.");
        }

        if (!Objects.equals(paymentKey, pgResponse.paymentKey())) {
            throw new IllegalArgumentException("PG 결제 키가 일치하지 않습니다.");
        }

        if (!Objects.equals(order.getOrderNo(), pgResponse.orderId())) {
            throw new IllegalArgumentException("PG 주문 번호가 일치하지 않습니다.");
        }

        if (!Objects.equals(order.getTotalPrice(), pgResponse.totalAmount())) {
            throw new IllegalArgumentException("PG 승인 금액이 일치하지 않습니다.");
        }

        if (!PG_APPROVED_STATUS.equals(pgResponse.status())) {
            throw new IllegalArgumentException("PG 결제 승인 상태가 올바르지 않습니다.");
        }
    }
    
    private void decreaseMenuStock(Order order) {
        List<OrderItems> orderItems = orderItemsRepository.findAllByOrderIdWithProduct(
                order.getOrderId()
        );

        if (orderItems.isEmpty()) {
            throw new IllegalStateException("주문 항목이 비어 있습니다.");
        }

        List<Long> menuIds = orderItems.stream()
                .map(orderItem -> orderItem.getProduct().getMenuId())
                .distinct()
                .sorted()
                .toList();

        List<Menu> lockedMenus = menuRepository.findAllByMenuIdsForUpdate(menuIds);

        Map<Long, Menu> menuMap = lockedMenus.stream()
                .collect(Collectors.toMap(Menu::getMenuId, menu -> menu));

        for (OrderItems orderItem : orderItems) {
            Menu product = orderItem.getProduct();
            Long menuId = product.getMenuId();

            Menu lockedMenu = menuMap.get(menuId);

            if (lockedMenu == null) {
                throw new IllegalArgumentException("메뉴 정보를 찾을 수 없습니다. menuId=" + menuId);
            }

            lockedMenu.decreaseStock(orderItem.getQuantity());
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

    private String maskPaymentKey(String paymentKey) {
        if (paymentKey == null || paymentKey.length() <= 12) {
            return "***";
        }
        return paymentKey.substring(0, 6) + "..." + paymentKey.substring(paymentKey.length() - 4);
    }

}
