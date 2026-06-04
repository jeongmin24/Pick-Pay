package com.ssafy.payclient.data.network

import com.ssafy.payclient.BuildConfig
import com.ssafy.payclient.data.api.AuthApiService
import com.ssafy.payclient.data.api.ReviewApiService
import com.ssafy.payclient.data.local.TokenManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Provider

object RetrofitClient {
    private const val BASE_URL = BuildConfig.BASE_URL
    private var authApiService: AuthApiService? = null
    private var reviewApiService: ReviewApiService? = null
    private var sharedTokenManager: TokenManager? = null

    fun init(tokenManager: TokenManager) {
        if (this.sharedTokenManager == null) {
            this.sharedTokenManager = tokenManager
        }
    }

    fun getApiService(tokenManager: TokenManager): AuthApiService {
        init(tokenManager)
        return authApiService ?: synchronized(this) {
            authApiService ?: buildAuthHttpClient().let { client ->
                buildRetrofit(client).create(AuthApiService::class.java).also { authApiService = it }
            }
        }
    }

    fun getReviewApiService(tokenManager: TokenManager): ReviewApiService {
        init(tokenManager)
        return reviewApiService ?: synchronized(this) {
            reviewApiService ?: buildHttpClient().let { client ->
                buildRetrofit(client).create(ReviewApiService::class.java).also { reviewApiService = it }
            }
        }
    }

    // 공통 OkHttpClient 빌더
    private fun buildAuthHttpClient(): OkHttpClient {
        val tm = sharedTokenManager ?: throw IllegalStateException("TokenManager가 초기화되지 않았습니다.")
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tm)) // 로그인 때 쓰던 바로 그 객체가 바인딩됨!
            .addInterceptor(loggingInterceptor)
            .build()
    }

    private fun buildHttpClient(): OkHttpClient {
        val tm = sharedTokenManager ?: throw IllegalStateException("TokenManager가 초기화되지 않았습니다.")
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tm)) // 로그인 때 쓰던 바로 그 객체가 바인딩됨!
            .addInterceptor(loggingInterceptor)
            .authenticator(TokenAuthenticator(tm, Provider { getApiService(tm) }))
            .build()
    }

    // 공통 Retrofit 빌더
    private fun buildRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
    }
}

