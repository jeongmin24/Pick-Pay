package com.ssafy.payclient.data.model

data class MenuDTO(
    val menuId: Long,
    val name: String,
    val price: Long,
    val stockQuantity : Int,
    val imageUrl: String
)