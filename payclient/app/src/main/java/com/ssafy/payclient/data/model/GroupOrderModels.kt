package com.ssafy.payclient.data.model

// 1. POST /api/groups

data class UserDto(
    val userId: Long,
    val loginId: String?,
    val nickname: String?,
    val fcmToken: String?,
    val createdAt: String?,
    val imageUrl: String?
)

// 방 생성 응답
data class GroupOrderResponse(
    val groupId: Long,
    val host: UserDto?,
    val shareLink: String?,
    val status: String?,
    val pickupUser: UserDto?,
    val payType: String?,
    val createdAt: String?
)

// 2. GET /api/groups/{groupId}/receipt
data class ReceiptResponseDTO(
    val groupId: Long,
    val payType: String?,
    val totalGroupPrice: Long,
    val userReceipts: List<UserReceiptDTO>
)

data class UserReceiptDTO(
    val userId: Long,
    val nickname: String?,
    val userTotalPrice: Long,
    val items: List<OrderItemDTO>
)

data class OrderItemDTO(
    val menuName: String?,
    val quantity: Int,
    val price: Long
)

data class CloseGroupOrderRequest(
    val payType: String
)