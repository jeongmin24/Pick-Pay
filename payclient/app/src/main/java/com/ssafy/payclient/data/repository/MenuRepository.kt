package com.ssafy.payclient.data.repository

import com.ssafy.payclient.data.model.MenuDTO
import kotlinx.coroutines.delay

class MenuRepository {

    // 임시 더미데이터 리포지토리
    suspend fun getMenus(): List<MenuDTO> {

        // 실제 인터넷 통신이 걸리는 시간(1초)을 흉내
        delay(1000)


        return listOf(
            MenuDTO(101L, "아이스 아메리카노", 4500),
            MenuDTO(102L, "카페라떼", 5000),
            MenuDTO(103L, "바닐라 라떼", 5500),
            MenuDTO(104L, "녹차 프라푸치노", 6000),
            MenuDTO(105L, "치즈케이크", 6500),
            MenuDTO(106L, "초코 머핀", 3500)
        )
    }
}