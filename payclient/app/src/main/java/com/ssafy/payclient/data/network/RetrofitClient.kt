package com.ssafy.payclient.data.network

import com.ssafy.payclient.BuildConfig
import com.ssafy.payclient.data.api.AuthApiService
import com.ssafy.payclient.data.local.TokenManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Provider

object RetrofitClient {
    private const val BASE_URL = BuildConfig.BASE_URL

    private var authApiService: AuthApiService? = null

    fun getApiService(tokenManager: TokenManager): AuthApiService {
        return authApiService ?: synchronized(this) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(tokenManager))
                .addInterceptor(loggingInterceptor)
                .authenticator(TokenAuthenticator(tokenManager, Provider { getApiService(tokenManager) }))
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()

            retrofit.create(AuthApiService::class.java).also { authApiService = it }
        }
    }
}
