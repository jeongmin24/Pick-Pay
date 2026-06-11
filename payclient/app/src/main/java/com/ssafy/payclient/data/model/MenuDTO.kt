package com.ssafy.payclient.data.model

// 메뉴판 데이터 조회 DTO -> 현재 repository에서 더미데이터 사용중
data class MenuDTO(
    val menuId: Long,
    val menuName: String,
    val price: Long,
    val imageResId: Int? = null
)