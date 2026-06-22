package com.ssafy.payclient.data.repository

import com.ssafy.payclient.data.model.MenuDTO
import kotlinx.coroutines.delay

class MenuRepository {

    // 임시 더미데이터 리포지토리
    suspend fun getMenus(): List<MenuDTO> {

        // 실제 인터넷 통신이 걸리는 시간(1초)을 흉내
        delay(1000)


        return listOf(
            MenuDTO(1L, "아이스 아메리카노", 4500),
            MenuDTO(2L, "아이스 카페라떼", 7000),
            MenuDTO(3L, "민트초코 프라푸치노", 15000),
            MenuDTO(4L, "녹차 프라푸치노", 5000),
            MenuDTO(5L, "치즈케이크", 7500),
        )
    }
}