package com.ssafy.payclient.ui.menu

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.payclient.data.local.PersonalCartItem
import com.ssafy.payclient.data.local.PersonalCartStore
import com.ssafy.payclient.data.local.TokenManager
import com.ssafy.payclient.data.repository.MenuRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.security.MessageDigest

private const val TAG = "싸피 MenuViewModel"

class MenuViewModel(private val tokenManager: TokenManager): ViewModel() {

    private val menuRepository = MenuRepository(tokenManager)
    private val _menuState = MutableStateFlow<MenuUiState>(MenuUiState.Loading)

    val menuState: StateFlow<MenuUiState> = _menuState.asStateFlow()

    private val _nfcEvent = MutableSharedFlow<NfcResult>()
    val nfcEvent: SharedFlow<NfcResult> = _nfcEvent.asSharedFlow()

    init {
        fetchMenus()
    }

    private fun fetchMenus() {
        viewModelScope.launch {
            _menuState.value = MenuUiState.Loading
            try {
                val response = menuRepository.getMenus()

                _menuState.value = MenuUiState.Success(response)
            } catch (e: Exception) {
                _menuState.value = MenuUiState.Error(e.message ?: "메뉴를 불러오는 중 오류가 발생했습니다.")
            }
        }
    }

    fun onMenuNfcScanned(menuId: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val menu = menuRepository.getMenuById(menuId)

                PersonalCartStore.add(menu, 1)

                Log.d(TAG, "NFC 장바구니 추가 성공: ${menu.name}")

                onResult(true)
            } catch (e: Exception) {
                Log.e(TAG, "NFC 장바구니 추가 실패: ${e.message}")
                onResult(false)
            }
        }
    }

    fun addAnalyzedMenusToCart(detectedMenu: String, onResult: (successCount: Int, failMenus: List<String>) -> Unit) {
        if (detectedMenu.isEmpty()) {
            onResult(0, emptyList())
            return
        }

        viewModelScope.launch {
            val menuNames = detectedMenu.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            var successCount = 0
            val failMenus = mutableListOf<String>()

            for (name in menuNames) {
                try {
                    val menu = menuRepository.getMenuByName(name)

                    PersonalCartStore.add(menu, 1)
                    successCount++
                    Log.d(TAG, "AI 장바구니 추가 성공: ${menu.name}")
                } catch (e: Exception) {
                    Log.e(TAG, "AI 장바구니 추가 실패 ($name): ${e.message}")
                    failMenus.add(name)
                }
            }
            onResult(successCount, failMenus)
        }
    }
}

sealed interface NfcResult {
    data class Success(val message: String) : NfcResult
    data class Error(val message: String) : NfcResult
}