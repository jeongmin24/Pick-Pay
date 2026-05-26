package com.ssafy.payclient

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ssafy.payclient.data.local.TokenManager
import com.ssafy.payclient.data.network.RetrofitClient
import com.ssafy.payclient.data.repository.AuthRepository
import com.ssafy.payclient.databinding.ActivityMainBinding
import com.ssafy.payclient.ui.login.LoginActivity
import com.ssafy.payclient.util.UiState
import com.ssafy.payclient.util.ViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels {
        val tokenManager = TokenManager(applicationContext)
        val apiService = RetrofitClient.getApiService(tokenManager)
        val repository = AuthRepository(apiService)
        ViewModelFactory(repository, tokenManager)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
        observeViewModel()
    }

    private fun initViews() {
        binding.btnLogout.setOnClickListener {
            viewModel.logout()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.logoutState.collect { state ->
                when (state) {
                    is UiState.Loading -> {
                        binding.btnLogout.isEnabled = false
                    }
                    is UiState.Success -> {
                        navigateToLogin()
                    }
                    is UiState.Error -> {
                        binding.btnLogout.isEnabled = true
                        Toast.makeText(this@MainActivity, state.message, Toast.LENGTH_SHORT).show()
                        // 실패하더라도 로그아웃 처리를 하고 싶다면 여기서 navigateToLogin() 호출 가능
                    }
                    else -> {}
                }
            }
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
