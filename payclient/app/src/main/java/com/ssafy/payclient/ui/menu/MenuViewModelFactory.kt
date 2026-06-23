package com.ssafy.payclient.ui.menu

import com.ssafy.payclient.data.local.TokenManager

class MenuViewModelFactory(
    private val tokenManager: TokenManager
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(com.ssafy.payclient.ui.menu.MenuViewModel::class.java)) {
            return com.ssafy.payclient.ui.menu.MenuViewModel(tokenManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}