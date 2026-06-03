package com.ssafy.payclient.data.model

data class MenuDTO(
    val menuId: Long,
    val menuName: String,
    val price: Int,
    val imageResId: Int? = null
)