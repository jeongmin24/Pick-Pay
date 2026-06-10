package com.ssafy.payclient.data.model

// 단체 주문 API용 DTO

// 방 생성 POST /api/groups
data class GroupOrderCreateResponse(
    val groupId: Long,
    val shareLink: String?,
    val status: String?,
    val host: Boolean
)

// 방 입장 POST /api/groups/join -> 방 생성 응답은 shareLink를 받지만 방 입장 요청은 shareToken만 서버로 보냄
data class GroupJoinRequest(
    val shareToken: String
)

data class GroupJoinResponse(
    val groupId: Long,
    val status: String,
    val host: Boolean
)

// GET /api/groups/{groupId}/receipt
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