package com.ssafy.payclient.data.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class FirebaseChatMessage(
    val senderId: Long = -1L,
    val senderName: String = "",
    val message: String = "",
    val createdAt: Long = 0L
)
