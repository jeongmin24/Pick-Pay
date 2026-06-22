package com.ssafy.payclient.data.api

import com.ssafy.payclient.data.model.IndividualOrderCreateResponseDTO
import com.ssafy.payclient.data.model.IndividualOrderRequestDTO
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface IndividualOrderApiService {

    @POST("/api/orders")
    suspend fun createOrder(
        @Body request: IndividualOrderRequestDTO
    ): Response<IndividualOrderCreateResponseDTO>
}
