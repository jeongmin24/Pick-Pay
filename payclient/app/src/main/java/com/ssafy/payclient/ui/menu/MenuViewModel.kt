package com.ssafy.payclient.ui.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.payclient.data.local.TokenManager
import com.ssafy.payclient.data.repository.MenuRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MenuViewModel(private val tokenManager: TokenManager): ViewModel() {

    private val menuRepository = MenuRepository(tokenManager)
    // 내부에서만 수정 가능한 상태 (초기값은 Loading)
    private val _menuState = MutableStateFlow<MenuUiState>(MenuUiState.Loading)

    // 외부(Fragment)에서 관찰만 가능한 상태
    val menuState: StateFlow<MenuUiState> = _menuState.asStateFlow()

    init {
        // ViewModel이 생성될 때 자동으로 메뉴 목록을 불러옵니다.
        fetchMenus()
    }

    private fun fetchMenus() {
        viewModelScope.launch {
            _menuState.value = MenuUiState.Loading
            try {
                // Repository를 통해 서버에서 데이터 가져오기
                val response = menuRepository.getMenus()

                // 성공 시 상태 업데이트
                _menuState.value = MenuUiState.Success(response)
            } catch (e: Exception) {
                // 실패 시 에러 메시지와 함께 상태 업데이트
                _menuState.value = MenuUiState.Error(e.message ?: "메뉴를 불러오는 중 오류가 발생했습니다.")
            }
        }
    }
}