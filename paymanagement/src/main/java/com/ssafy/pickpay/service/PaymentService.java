package com.ssafy.pickpay.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.ssafy.pickpay.common.OrderStatus;
import com.ssafy.pickpay.domain.GroupOrder;
import com.ssafy.pickpay.domain.Menu;
import com.ssafy.pickpay.domain.Order;
import com.ssafy.pickpay.domain.OrderItems;
import com.ssafy.pickpay.dto.PaymentCompleteRequestDTO;
import com.ssafy.pickpay.dto.PaymentCompleteResponseDTO;
import com.ssafy.pickpay.dto.PgConfirmResponse;
import com.ssafy.pickpay.infra.pg.PgPaymentClient;
import com.ssafy.pickpay.repository.MenuRepository;
import com.ssafy.pickpay.repository.OrderItemsRepository;
import com.ssafy.pickpay.repository.OrderRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service @RequiredArgsConstructor
public class PaymentService {
	
	private final OrderRepository orderRepository;
	private final OrderItemsRepository orderItemsRepository;
	private final MenuRepository menuRepository;
	private final PgPaymentClient pgPaymentClient;
	
	@Transactional
    public PaymentCompleteResponseDTO completePayment(
            Long userId,
            PaymentCompleteRequestDTO request
    ) {
        Order order = orderRepository.findByOrderNoForUpdate(request.orderId())
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

        validateOrderOwner(order, userId);

        if (order.isPaid()) {
            return new PaymentCompleteResponseDTO(
                    order.getOrderNo(),
                    order.getTotalPrice(),
                    order.getStatus().name(),
                    "이미 결제 완료된 주문입니다."
            );
        }

        validateAmount(order, request.amount());

        PgConfirmResponse pgResponse = pgPaymentClient.confirmPayment(
                request.paymentKey(),
                request.orderId(),
                request.amount()
        );

        validatePgResponse(order, pgResponse);

        decreaseMenuStock(order);

        order.markPaid();

        updateGroupOrderIfNeeded(order);

        return new PaymentCompleteResponseDTO(
                order.getOrderNo(),
                order.getTotalPrice(),
                order.getStatus().name(),
                "결제가 완료되었습니다."
        );
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

    private void validatePgResponse(Order order, PgConfirmResponse pgResponse) {
        if (!Objects.equals(order.getOrderNo(), pgResponse.orderId())) {
            throw new IllegalArgumentException("PG 주문 번호가 일치하지 않습니다.");
        }

        if (!Objects.equals(order.getTotalPrice(), pgResponse.totalAmount())) {
            throw new IllegalArgumentException("PG 승인 금액이 일치하지 않습니다.");
        }

        if (!"DONE".equals(pgResponse.status()) && !"PAID".equals(pgResponse.status())) {
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


}
