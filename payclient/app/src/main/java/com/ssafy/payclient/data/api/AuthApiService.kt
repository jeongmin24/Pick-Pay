package com.ssafy.payclient.data.api

import com.ssafy.payclient.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface AuthApiService {
    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("user/exist")
    suspend fun checkUserExists(@Body request: IdCheckRequest): Response<Boolean>

    @POST("user")
    suspend fun signup(@Body request: SignupRequest): Response<Unit>

    @GET("user")
    suspend fun getMyInfo(): Response<UserResponse>

    @PUT("user")
    suspend fun updateMyInfo(@Body request: UserUpdateRequest): Response<UserResponse>

    @PATCH("me/fcm-token")
    suspend fun updateFcmToken(@Body request: FcmTokenRequest): Response<Unit>

    @DELETE("user")
    suspend fun deleteUser(): Response<Unit>

    @POST("logout")
    suspend fun logout(): Response<Unit>

    @POST("refresh")
    suspend fun refreshToken(@Header("Authorization") refreshToken: String): Response<LoginResponse>
}
