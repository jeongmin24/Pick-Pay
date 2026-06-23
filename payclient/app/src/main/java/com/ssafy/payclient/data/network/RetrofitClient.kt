package com.ssafy.payclient.data.network

import com.ssafy.payclient.BuildConfig
import com.ssafy.payclient.data.api.AuthApiService
import com.ssafy.payclient.data.api.GroupOrderApiService
import com.ssafy.payclient.data.api.IndividualOrderApiService
import com.ssafy.payclient.data.api.PaymentApiService
import com.ssafy.payclient.data.api.MenuApiService
import com.ssafy.payclient.data.api.ReviewApiService
import com.ssafy.payclient.data.api.UserApiService
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
    private var individualOrderApiService: IndividualOrderApiService? = null
    private var paymentApiService: PaymentApiService? = null
    private var sharedTokenManager: TokenManager? = null
    var menuApiService: MenuApiService? = null
    private var groupOrderApiService: GroupOrderApiService? = null
    private var userApiService: UserApiService? = null

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

    fun getUserApiService(tokenManager: TokenManager): UserApiService {
        init(tokenManager)
        return userApiService ?: synchronized(this) {
            userApiService ?: buildHttpClient().let { client ->
                buildRetrofit(client)
                    .create(UserApiService::class.java)
                    .also { userApiService = it }
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

    fun getIndividualOrderApiService(tokenManager: TokenManager): IndividualOrderApiService {
        init(tokenManager)
        return individualOrderApiService ?: synchronized(this) {
            individualOrderApiService ?: buildHttpClient().let { client ->
                buildRetrofit(client)
                    .create(IndividualOrderApiService::class.java)
                    .also { individualOrderApiService = it }
            }
        }
    }

    fun getGroupOrderApiService(tokenManager: TokenManager): GroupOrderApiService {
        init(tokenManager)

        return groupOrderApiService ?: synchronized(this) {
            groupOrderApiService ?: buildHttpClient().let { client ->
                buildRetrofit(client)
                    .create(GroupOrderApiService::class.java)
                    .also { groupOrderApiService = it }
            }
        }
    }

    fun getPaymentApiService(tokenManager: TokenManager): PaymentApiService {
        init(tokenManager)

        return paymentApiService ?: synchronized(this) {
            paymentApiService ?: buildHttpClient().let { client ->
                buildRetrofit(client)
                    .create(PaymentApiService::class.java)
                    .also { paymentApiService = it }
            }
        }
    }

    fun getMenuApiService(tokenManager: TokenManager): MenuApiService {
        init(tokenManager)
        return menuApiService ?: synchronized(this) {
            menuApiService ?: buildHttpClient().let { client ->
                buildRetrofit(client).create(MenuApiService::class.java).also { menuApiService = it }
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

