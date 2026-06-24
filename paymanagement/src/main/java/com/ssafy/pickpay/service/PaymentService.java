package com.ssafy.pickpay.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.ssafy.pickpay.common.ErrorCode;
import com.ssafy.pickpay.common.OrderStatus;
import com.ssafy.pickpay.domain.GroupOrder;
import com.ssafy.pickpay.domain.Menu;
import com.ssafy.pickpay.domain.Order;
import com.ssafy.pickpay.domain.OrderItems;
import com.ssafy.pickpay.domain.Payment;
import com.ssafy.pickpay.dto.PaymentCompleteRequestDTO;
import com.ssafy.pickpay.dto.PaymentCompleteResponseDTO;
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

        validateOrderPayable(order);
        validatePaymentNotExpired(order);
        validateAmount(order, request.amount());

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
            if (dbException instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, dbException);
        }
    }

    private PaymentCompleteResponseDTO completePaymentInTransaction(
            Long userId,
            PaymentCompleteRequestDTO request,
            PgConfirmResponse pgResponse
    ) {
        Order lockedOrder = orderRepository.findByOrderNoForUpdate(request.orderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        validateOrderOwner(lockedOrder, userId);
        if (lockedOrder.isPaid()) {
            return createAlreadyPaidResponse(lockedOrder);
        }

        validateOrderPayable(lockedOrder);
        validatePaymentNotExpired(lockedOrder);
        validateAmount(lockedOrder, request.amount());
        validatePgResponse(lockedOrder, request.paymentKey(), pgResponse);

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
        List<OrderItems> orderItems = orderItemsRepository.findAllByOrderIdWithProduct(
                order.getOrderId()
        );

        if (orderItems.isEmpty()) {
            throw new BusinessException(ErrorCode.PAYMENT_CONFLICT);
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
                throw new BusinessException(ErrorCode.PAYMENT_CONFLICT);
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

    private PaymentCompleteResponseDTO createAlreadyPaidResponse(Order order) {
        return new PaymentCompleteResponseDTO(
                order.getOrderNo(),
                order.getTotalPrice(),
                order.getStatus().name(),
                "이미 결제 완료된 주문입니다."
        );
    }

}
