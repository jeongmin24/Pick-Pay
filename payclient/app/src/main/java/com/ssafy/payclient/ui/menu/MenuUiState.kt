package com.ssafy.payclient.ui.menu

import com.ssafy.payclient.data.model.MenuDTO

sealed class MenuUiState {
    object Loading : MenuUiState()
    data class Success(val menuList: List<MenuDTO>) : MenuUiState()
    data class Error(val message: String) : MenuUiState()
}