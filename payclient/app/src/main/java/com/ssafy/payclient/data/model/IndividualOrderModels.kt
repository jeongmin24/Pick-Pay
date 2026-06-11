package com.ssafy.payclient.data.model

// 주문시 SQLite에 저장한 장바구니 리스트를 서버로 보냄
data class IndividualOrderRequest(
    val items: List<CartItemRequest>
)

data class CartItemRequest(
    val menuId: Long,
    val quantity: Int
)

// 주문 생성 응답 DTO
data class IndividualOrderCreateResponseDTO(
    val orderId: Long,
    val totalPrice: Long,
    val status: String
)

// 주문 완료 후 영수증 조회 GET /api/orders/{orderId}/receipt
data class IndividualReceiptResponseDTO(
    val orderId: Long,
    val totalPrice: Long,       // 내가 낸 총 금액
    val status: String,          // PAID, CANCELED 등
    val orderedAt: String,       // 주문 일시
    val items: List<OrderItemDTO> // 내가 주문한 메뉴 상세 리스트 (기존 DTO 재사용!)
)

// 영수증 응답 DTO
