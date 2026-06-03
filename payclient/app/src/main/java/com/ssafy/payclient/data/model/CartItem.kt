package com.ssafy.payclient.data.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class CartItem(
    val menuName: String = "",
    val menuId: Long = 0,
    val quantity: Int = 0,
    val userId: Long = 0
)