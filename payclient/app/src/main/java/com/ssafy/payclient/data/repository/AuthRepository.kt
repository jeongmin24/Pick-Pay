package com.ssafy.payclient.data.repository

import com.ssafy.payclient.data.api.AuthApiService
import com.ssafy.payclient.data.model.*
import retrofit2.Response

class AuthRepository(private val apiService: AuthApiService) {
    suspend fun login(request: LoginRequest): Response<LoginResponse> = apiService.login(request)
    
    suspend fun checkUserExists(loginId: String): Response<Boolean> = 
        apiService.checkUserExists(IdCheckRequest(loginId))
    
    suspend fun signup(request: SignupRequest): Response<Unit> = apiService.signup(request)

    suspend fun logout(): Response<Unit> = apiService.logout()
}
