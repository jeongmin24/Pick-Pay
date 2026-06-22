package com.ssafy.payclient.data.api

import com.ssafy.payclient.data.model.PaymentCompleteRequest
import com.ssafy.payclient.data.model.PaymentCompleteResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface PaymentApiService {

    @POST("/api/payments/complete")
    suspend fun completePayment(
        @Body request: PaymentCompleteRequest
    ): Response<PaymentCompleteResponse>
}
