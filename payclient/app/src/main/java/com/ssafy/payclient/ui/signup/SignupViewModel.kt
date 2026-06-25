package com.ssafy.payclient.ui.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.ssafy.payclient.data.model.ErrorResponse
import com.ssafy.payclient.data.model.SignupRequest
import com.ssafy.payclient.data.repository.AuthRepository
import com.ssafy.payclient.util.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SignupViewModel(private val repository: AuthRepository) : ViewModel() {

    private val gson = Gson()

    private val _signupState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val signupState: StateFlow<UiState<Unit>> = _signupState

    private val _idCheckState = MutableStateFlow<UiState<Boolean>>(UiState.Idle)
    val idCheckState: StateFlow<UiState<Boolean>> = _idCheckState

    fun checkId(loginId: String) {
        if (loginId.isBlank()) {
            _idCheckState.value = UiState.Error("아이디를 입력해주세요.")
            return
        }

        viewModelScope.launch {
            _idCheckState.value = UiState.Loading
            try {
                val response = repository.checkUserExists(loginId)
                if (response.isSuccessful && response.body() != null) {
                    val exists = response.body()!!
                    _idCheckState.value = UiState.Success(exists)
                } else {
                    _idCheckState.value = UiState.Error("중복 확인 실패")
                }
            } catch (e: Exception) {
                _idCheckState.value = UiState.Error("네트워크 오류: ${e.message}")
            }
        }
    }

    fun signup(request: SignupRequest) {
        if (request.loginId.isBlank() || request.password.isBlank() || request.nickname.isBlank()) {
            _signupState.value = UiState.Error("모든 필드를 입력해주세요.")
            return
        }

        viewModelScope.launch {
            _signupState.value = UiState.Loading
            try {
                val response = repository.signup(request)
                if (response.isSuccessful) {
                    _signupState.value = UiState.Success(Unit)
                } else {
                    val errorResponse = parseErrorResponse(response.errorBody()?.string())
                    _signupState.value = UiState.Error(
                        message = errorResponse?.message ?: "회원가입 실패: ${response.message()}",
                        fieldErrors = errorResponse?.errors.orEmpty()
                    )
                }
            } catch (e: Exception) {
                _signupState.value = UiState.Error("네트워크 오류: ${e.message}")
            }
        }
    }

    private fun parseErrorResponse(rawBody: String?): ErrorResponse? {
        if (rawBody.isNullOrBlank()) return null

        return runCatching {
            gson.fromJson(rawBody, ErrorResponse::class.java)
        }.getOrNull()
    }
}
