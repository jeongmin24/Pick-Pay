package com.ssafy.payclient.data.model

data class PaymentCompleteRequest(
    val paymentKey: String,
    val orderId: String,
    val amount: Long
)

data class PaymentCompleteResponse(
    val orderId: String,
    val amount: Long,
    val orderStatus: String,
    val message: String
)