package com.ssafy.payclient.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ssafy.payclient.MainActivity
import com.ssafy.payclient.data.local.TokenManager
import com.ssafy.payclient.data.network.RetrofitClient
import com.ssafy.payclient.data.repository.AuthRepository
import com.ssafy.payclient.databinding.ActivityLoginBinding
import com.ssafy.payclient.ui.signup.SignupActivity
import com.ssafy.payclient.util.UiState
import com.ssafy.payclient.util.ViewModelFactory
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels {
        val tokenManager = TokenManager(applicationContext)
        val apiService = RetrofitClient.getApiService(tokenManager)
        val repository = AuthRepository(apiService)
        ViewModelFactory(repository, tokenManager)
    }

    // C:\SSAFY\Pick_Pay\app\src\main\java\com\ssafy\payclient\ui\login\LoginActivity.kt

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 자동 로그인 체크: 저장된 AccessToken + RefreshToken이 있으면 바로 메인으로 이동
        val tokenManager = TokenManager(applicationContext)
        if (tokenManager.accessToken != null && tokenManager.getRefreshToken() != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
        observeViewModel()
    }

    private fun initViews() {
        binding.btnLogin.setOnClickListener {
            val id = binding.etLoginId.text.toString()
            val pw = binding.etPassword.text.toString()
            viewModel.login(id, pw)
        }

        binding.btnGoToSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.loginState.collect { state ->
                when (state) {
                    is UiState.Loading -> {
                        binding.pbLoading.visibility = View.VISIBLE
                        binding.btnLogin.isEnabled = false
                    }
                    is UiState.Success -> {
                        binding.pbLoading.visibility = View.GONE
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    }
                    is UiState.Error -> {
                        binding.pbLoading.visibility = View.GONE
                        binding.btnLogin.isEnabled = true
                        Toast.makeText(this@LoginActivity, state.message, Toast.LENGTH_SHORT).show()
                    }
                    is UiState.Idle -> {
                        binding.pbLoading.visibility = View.GONE
                    }
                }
            }
        }
    }
}
