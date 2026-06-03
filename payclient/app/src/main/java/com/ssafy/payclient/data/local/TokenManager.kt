package com.ssafy.payclient.data.local

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONObject

class TokenManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private var _accessToken: String? = null
    val accessToken: String? get() = _accessToken

    fun saveAccessToken(token: String) {
        _accessToken = token
    }

    fun saveRefreshToken(token: String) {
        sharedPreferences.edit().putString("refresh_token", token).apply()
    }

    fun getRefreshToken(): String? {
        return sharedPreferences.getString("refresh_token", null)
    }

    fun clear() {
        _accessToken = null
        sharedPreferences.edit().remove("refresh_token").apply()
    }

    fun getUserId(): Long {
        // 액세스 토큰이 없으면 -1 반환
        val token = _accessToken ?: return -1L

        return try {
            val split = token.split(".")
            if (split.size != 3) return -1L

            val payloadString = String(Base64.decode(split[1], Base64.URL_SAFE))
            val jsonObject = JSONObject(payloadString)

            jsonObject.getLong("userId")
        } catch (e: Exception) {
            e.printStackTrace()
            -1L
        }
    }
}
