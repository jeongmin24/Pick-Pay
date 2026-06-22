package com.ssafy.payclient.data.model

data class IndividualOrderRequestDTO(
    val items: List<CartItemRequest>
)

data class CartItemRequest(
    val menuId: Long,
    val quantity: Int
)

data class IndividualOrderCreateResponseDTO(
    val orderId: String,
    val totalPrice: Long,
    val status: String
)

data class IndividualReceiptResponseDTO(
    val orderId: Long,
    val totalPrice: Long,
    val status: String,
    val orderedAt: String,
    val items: List<OrderItemDTO>
)
