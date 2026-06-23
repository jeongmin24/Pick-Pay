package com.ssafy.payclient.data.api

import com.ssafy.payclient.data.model.MenuDTO
import retrofit2.http.GET
import retrofit2.http.Path

interface MenuApiService {
    @GET("api/menus")
    suspend fun getMenus(): List<MenuDTO>

    @GET("api/menus/{menuId}")
    suspend fun getMenuById(@Path("menuId") menuId: Long): MenuDTO
}