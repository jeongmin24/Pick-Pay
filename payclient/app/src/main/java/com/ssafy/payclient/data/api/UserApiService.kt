package com.ssafy.payclient.data.api

import com.ssafy.payclient.data.model.UserResponse
import retrofit2.http.GET

interface UserApiService {
    @GET("/user")
    suspend fun getUserInfo(): UserResponse

}