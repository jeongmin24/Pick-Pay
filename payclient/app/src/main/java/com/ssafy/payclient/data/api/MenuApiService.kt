package com.ssafy.payclient.data.api

import com.ssafy.payclient.data.model.MenuDTO
import retrofit2.http.GET

interface MenuApiService {
    @GET("api/menus")
    suspend fun getMenus(): List<MenuDTO>
}