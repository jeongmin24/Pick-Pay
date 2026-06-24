package com.ssafy.payclient.data.model

data class PaymentCompleteRequest(
    val paymentKey: String,
    val orderId: String,
    val amount: Long
)

data class PaymentFailRequest(
    val orderId: String,
    val reason: String
)

data class PaymentCompleteResponse(
    val orderId: String,
    val amount: Long,
    val orderStatus: String,
    val groupId: String?,
    val groupStatus: String?,
    val message: String
)
