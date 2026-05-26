package com.ssafy.payclient.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ssafy.payclient.MainViewModel
import com.ssafy.payclient.data.local.TokenManager
import com.ssafy.payclient.data.repository.AuthRepository
import com.ssafy.payclient.ui.login.LoginViewModel
import com.ssafy.payclient.ui.signup.SignupViewModel

class ViewModelFactory(
    private val repository: AuthRepository,
    private val tokenManager: TokenManager? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(LoginViewModel::class.java) -> {
                LoginViewModel(repository, tokenManager!!) as T
            }
            modelClass.isAssignableFrom(SignupViewModel::class.java) -> {
                SignupViewModel(repository) as T
            }
            modelClass.isAssignableFrom(MainViewModel::class.java) -> {
                MainViewModel(repository, tokenManager!!) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
