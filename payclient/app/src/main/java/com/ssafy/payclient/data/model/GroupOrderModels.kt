package com.ssafy.payclient.data.model

/**
 * 단체 주문 관리용 API DTO
 */

// 방 생성 POST /api/groups
data class GroupOrderCreateResponse(
    val groupId: String,
    val shareLink: String?,
    val status: String?,
    val host: Boolean
)

// 방 입장 POST /api/groups/join -> 방 생성 응답은 shareLink를 받지만 방 입장 요청은 shareToken만 서버로 보냄
data class GroupJoinRequest(
    val shareToken: String
)

data class GroupJoinResponse(
    val groupId: String,
    val status: String,
    val host: Boolean
)

enum class GroupPayType {
    DUTCH,
    HOST
}

// 방 마감 (호스트가 결제 방식을 결정) POST /api/groups/{groupId}/close
data class CloseGroupOrderRequest(
    val payType: GroupPayType
)

data class PickupCandidate(
    val userId: Long,
    val nickname: String?
)

data class PickupRouletteResponse(
    val groupId: String = "",
    val roundId: String = "",
    val status: String = "",
    val startedAt: Long = 0L,
    val durationMs: Long = 3200L,
    val winnerUserId: Long = -1L,
    val winnerNickname: String? = null,
    val winnerIndex: Int = 0,
    val alreadySelected: Boolean = false,
    val chatPushed: Boolean = false,
    val candidates: List<PickupCandidate> = emptyList()
)

/**
 * 단체주문 영수증 조회 API DTO
 */


// 방 전체 영수증 GET /api/groups/{groupId}/receipt
data class ReceiptResponseDTO(
    val groupId: String,
    val payType: String?,
    val groupStatus: String?,
    val totalGroupPrice: Long,
    val userReceipts: List<UserReceiptDTO>
)

// 개별 유저 영수증
data class UserReceiptDTO(
    val userId: Long,
    val orderNo: String?,
    val displayOrderNo: String?,
    val nickname: String?,
    val orderStatus: String?,
    val userTotalPrice: Long,
    val items: List<OrderItemDTO>
)

// 영수증 세뷰 메뉴
data class OrderItemDTO(
    val menuName: String?,
    val quantity: Int,
    val price: Long
)

