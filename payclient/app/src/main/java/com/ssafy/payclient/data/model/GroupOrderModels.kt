package com.ssafy.payclient.data.model

/**
 * 단체 주문 관리용 API DTO
 */

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

// 방 마감 (호스트가 결제 방식을 결정) PUT /api/groups/{groupId}/close
data class CloseGroupOrderRequest(
    val payType: String
)

/**
 * 단체주문 영수증 조회 API DTO
 */


// 방 전체 영수증 GET /api/groups/{groupId}/receipt
data class ReceiptResponseDTO(
    val groupId: Long,
    val payType: String?,
    val totalGroupPrice: Long,
    val userReceipts: List<UserReceiptDTO>
)

// 개별 유저 영수증
data class UserReceiptDTO(
    val userId: Long,
    val nickname: String?,
    val userTotalPrice: Long,
    val items: List<OrderItemDTO>
)

// 영수증 세뷰 메뉴
data class OrderItemDTO(
    val menuName: String?,
    val quantity: Int,
    val price: Long
)

