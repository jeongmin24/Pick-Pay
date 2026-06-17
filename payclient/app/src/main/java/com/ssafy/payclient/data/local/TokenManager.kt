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

    val accessToken: String?
        get() = sharedPreferences.getString("access_token", null)

    fun saveAccessToken(token: String) {
        sharedPreferences.edit()
            .putString("access_token", token)
            .apply()
    }


    fun saveRefreshToken(token: String) {
        sharedPreferences.edit().putString("refresh_token", token).apply()
    }

    fun getRefreshToken(): String? {
        return sharedPreferences.getString("refresh_token", null)
    }

    fun clear() {
        sharedPreferences.edit()
            .remove("access_token")
            .remove("refresh_token")
            .apply()
    }

    fun getUserId(): Long {
        val token = accessToken ?: return -1L

        return try {
            val split = token.split(".")
            if (split.size != 3) return -1L

            val payloadString = String(
                Base64.decode(
                    split[1],
                    Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
                )
            )

            val jsonObject = JSONObject(payloadString)
            jsonObject.getLong("userId")

        } catch (e: Exception) {
            e.printStackTrace()
            -1L
        }
    }

}
