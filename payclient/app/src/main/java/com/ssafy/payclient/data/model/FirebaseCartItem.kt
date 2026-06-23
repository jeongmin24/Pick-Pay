package com.ssafy.payclient.data.model

import com.google.firebase.database.IgnoreExtraProperties

// FirebaseDB 읽고 쓰기 전용 장바구니
@IgnoreExtraProperties
data class FirebaseCartItem(
    val menuName: String = "",
    val productId: Long = 0,
    val quantity: Int = 0,
    val userId: Long = 0
)
