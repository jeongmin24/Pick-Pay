package com.ssafy.payclient.ui.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.payclient.data.local.TokenManager
import com.ssafy.payclient.data.model.LoginRequest
import com.ssafy.payclient.data.repository.AuthRepository
import com.ssafy.payclient.util.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val TAG = "LoginViewModel_싸피"
class LoginViewModel(
    private val repository: AuthRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _loginState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val loginState: StateFlow<UiState<Unit>> = _loginState

    fun login(loginId: String, password: String) {
        if (loginId.isBlank() || password.isBlank()) {
            _loginState.value = UiState.Error("아이디와 비밀번호를 입력해주세요.")
            return
        }

        viewModelScope.launch {
            _loginState.value = UiState.Loading
            try {
                val response = repository.login(LoginRequest(loginId, password))
                if (response.isSuccessful && response.body() != null) {
                    val tokens = response.body()!!
                    tokenManager.saveAccessToken(tokens.accessToken)
                    tokenManager.saveRefreshToken(tokens.refreshToken)
                    Log.d(TAG, "accessToken=${tokenManager.accessToken?.take(20)}")
                    Log.d(TAG, "userId=${tokenManager.getUserId()}")
                    _loginState.value = UiState.Success(Unit)
                } else {
                    _loginState.value = UiState.Error("로그인 실패: ${response.message()}")
                }
            } catch (e: Exception) {
                _loginState.value = UiState.Error("네트워크 오류: ${e.message}")
            }
        }
    }
}
