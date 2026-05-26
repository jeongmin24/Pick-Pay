package com.ssafy.payclient.data.network

import com.ssafy.payclient.data.api.AuthApiService
import com.ssafy.payclient.data.local.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Provider

class TokenAuthenticator(
    private val tokenManager: TokenManager,
    private val authApiService: Provider<AuthApiService>
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Only try to refresh if the request was unauthorized (401)
        if (response.code != 401) return null

        val refreshToken = tokenManager.getRefreshToken() ?: return null

        synchronized(this) {
            // Check if the token was already refreshed by another thread
            val currentAccessToken = tokenManager.accessToken
            val requestToken = response.request.header("Authorization")
            
            if (requestToken != "Bearer $currentAccessToken") {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentAccessToken")
                    .build()
            }

            // Perform token refresh blocking the thread
            return runBlocking {
                try {
                    val refreshResponse = authApiService.get().refreshToken("Bearer $refreshToken")
                    if (refreshResponse.isSuccessful && refreshResponse.body() != null) {
                        val newTokens = refreshResponse.body()!!
                        tokenManager.saveAccessToken(newTokens.accessToken)
                        tokenManager.saveRefreshToken(newTokens.refreshToken)

                        response.request.newBuilder()
                            .header("Authorization", "Bearer ${newTokens.accessToken}")
                            .build()
                    } else {
                        tokenManager.clear()
                        // Here you might want to trigger a logout/re-navigation to Login screen
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
}
