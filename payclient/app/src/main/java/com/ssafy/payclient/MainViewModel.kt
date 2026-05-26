package com.ssafy.payclient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.payclient.data.local.TokenManager
import com.ssafy.payclient.data.repository.AuthRepository
import com.ssafy.payclient.util.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: AuthRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _logoutState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val logoutState: StateFlow<UiState<Unit>> = _logoutState

    fun logout() {
        viewModelScope.launch {
            _logoutState.value = UiState.Loading
            try {
                // 서버에 로그아웃 요청 (실패하더라도 로컬 토큰은 삭제하는 것이 일반적입니다)
                repository.logout()
                tokenManager.clear()
                _logoutState.value = UiState.Success(Unit)
            } catch (e: Exception) {
                // 네트워크 오류 등이 발생해도 일단 로컬 세션은 종료합니다.
                tokenManager.clear()
                _logoutState.value = UiState.Success(Unit)
            }
        }
    }
}
