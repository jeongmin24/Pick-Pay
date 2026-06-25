package com.ssafy.payclient.data.api

import com.ssafy.payclient.data.model.IndividualOrderCreateResponseDTO
import com.ssafy.payclient.data.model.IndividualReceiptResponseDTO
import com.ssafy.payclient.data.model.IndividualOrderRequestDTO
import com.ssafy.payclient.data.model.RecentOrderResponseDTO
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.Query

interface IndividualOrderApiService {

    @POST("/api/orders")
    suspend fun createOrder(
        @Body request: IndividualOrderRequestDTO
    ): Response<IndividualOrderCreateResponseDTO>

    @GET("/api/orders/{orderNo}/receipt")
    suspend fun getReceipt(
        @Path("orderNo") orderNo: String
    ): Response<IndividualReceiptResponseDTO>

    @GET("/api/orders/recent")
    suspend fun getRecentOrders(
        @Query("limit") limit: Int = 10
    ): Response<List<RecentOrderResponseDTO>>
}
