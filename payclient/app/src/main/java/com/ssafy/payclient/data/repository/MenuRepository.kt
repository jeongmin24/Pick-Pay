package com.ssafy.payclient.data.repository

import com.ssafy.payclient.data.local.TokenManager
import com.ssafy.payclient.data.model.MenuDTO
import com.ssafy.payclient.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MenuRepository(private val tokenManager: TokenManager) {

    suspend fun getMenus(): List<MenuDTO> {

        return withContext(Dispatchers.IO) {
            try {
                val menuService = RetrofitClient.getMenuApiService(tokenManager)
                menuService.getMenus()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
}