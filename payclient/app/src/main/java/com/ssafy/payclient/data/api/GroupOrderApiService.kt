package com.ssafy.payclient.data.api

import com.ssafy.payclient.data.model.GroupOrderResponse
import retrofit2.Response
import retrofit2.http.POST

interface GroupOrderApiService {

    @POST("/api/groups")
    suspend fun createGroupOrder(): Response<GroupOrderResponse>
}