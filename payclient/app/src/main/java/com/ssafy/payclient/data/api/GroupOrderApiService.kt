package com.ssafy.payclient.data.api

import com.ssafy.payclient.data.model.CloseGroupOrderRequest
import com.ssafy.payclient.data.model.GroupJoinRequest
import com.ssafy.payclient.data.model.GroupJoinResponse
import com.ssafy.payclient.data.model.GroupOrderCreateResponse
import com.ssafy.payclient.data.model.PickupRouletteResponse
import com.ssafy.payclient.data.model.ReceiptResponseDTO
import retrofit2.Response
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface GroupOrderApiService {

    @POST("/api/groups")
    suspend fun createGroupOrder(): Response<GroupOrderCreateResponse>

    @POST("/api/groups/join")
    suspend fun joinGroup(
        @Body request: GroupJoinRequest
    ): Response<GroupJoinResponse>

    @POST("/api/groups/{groupId}/close")
    suspend fun closeGroupOrder(
        @Path("groupId") groupId: String,
        @Body request: CloseGroupOrderRequest
    ): Response<ResponseBody>

    @GET("/api/groups/{groupId}/receipt")
    suspend fun getGroupReceipt(
        @Path("groupId") groupId: String
    ): Response<ReceiptResponseDTO>

    @POST("/api/groups/{groupId}/pickup-roulette")
    suspend fun selectPickupWinner(
        @Path("groupId") groupId: String
    ): Response<PickupRouletteResponse>

}
