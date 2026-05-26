package com.ssafy.payclient.data.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val loginId: String,
    val password: String
)

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String
)

data class SignupRequest(
    val loginId: String,
    val password: String,
    val nickname: String
)

data class IdCheckRequest(
    val loginId: String
)

data class UserResponse(
    val loginId: String,
    val nickname: String
)

data class UserUpdateRequest(
    val loginId: String,
    val nickname: String
)
