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
    val displayOrderNo: String?,
    val totalPrice: Long,
    val status: String
)

data class IndividualReceiptResponseDTO(
    val orderId: Long,
    val displayOrderNo: String?,
    val totalPrice: Long,
    val status: String,
    val createdAt: String,
    val items: List<OrderItemDTO>
)

data class RecentOrderResponseDTO(
    val orderNo: String,
    val displayOrderNo: String?,
    val totalPrice: Long,
    val status: String,
    val createdAt: String,
    val firstMenuName: String?,
    val totalQuantity: Int,
    val itemCount: Int
)
