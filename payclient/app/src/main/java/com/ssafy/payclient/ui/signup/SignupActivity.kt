package com.ssafy.payclient.ui.signup

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ssafy.payclient.data.local.TokenManager
import com.ssafy.payclient.data.model.SignupRequest
import com.ssafy.payclient.data.network.RetrofitClient
import com.ssafy.payclient.data.repository.AuthRepository
import com.ssafy.payclient.databinding.ActivitySignupBinding
import com.ssafy.payclient.util.UiState
import com.ssafy.payclient.util.ViewModelFactory
import kotlinx.coroutines.launch

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private val viewModel: SignupViewModel by viewModels {
        val tokenManager = TokenManager(applicationContext)
        val apiService = RetrofitClient.getApiService(tokenManager)
        val repository = AuthRepository(apiService)
        ViewModelFactory(repository)
    }

    private var isIdChecked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
        observeViewModel()
    }

    private fun initViews() {
        binding.btnCheckId.setOnClickListener {
            val id = binding.etSignupId.text.toString()
            viewModel.checkId(id)
        }

        binding.btnSignup.setOnClickListener {
            if (!isIdChecked) {
                Toast.makeText(this, "아이디 중복 확인을 해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val id = binding.etSignupId.text.toString()
            val pw = binding.etSignupPassword.text.toString()
            val pwConfirm = binding.etSignupPasswordConfirm.text.toString()
            val nickname = binding.etSignupNickname.text.toString()

            if (id.isBlank() || pw.isBlank() || nickname.isBlank()) {
                Toast.makeText(this, "모든 필드를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pw != pwConfirm) {
                binding.tilSignupPasswordConfirm.error = "비밀번호가 일치하지 않습니다."
                return@setOnClickListener
            } else {
                binding.tilSignupPasswordConfirm.error = null
            }
            
            viewModel.signup(SignupRequest(id, pw, nickname))
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.idCheckState.collect { state ->
                when (state) {
                    is UiState.Loading -> {
                        binding.pbLoading.visibility = View.VISIBLE
                    }
                    is UiState.Success -> {
                        binding.pbLoading.visibility = View.GONE
                        val exists = state.data
                        if (exists) {
                            isIdChecked = false
                            Toast.makeText(this@SignupActivity, "이미 존재하는 아이디입니다.", Toast.LENGTH_SHORT).show()
                        } else {
                            isIdChecked = true
                            Toast.makeText(this@SignupActivity, "사용 가능한 아이디입니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    is UiState.Error -> {
                        binding.pbLoading.visibility = View.GONE
                        Toast.makeText(this@SignupActivity, state.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }

        lifecycleScope.launch {
            viewModel.signupState.collect { state ->
                when (state) {
                    is UiState.Loading -> {
                        binding.pbLoading.visibility = View.VISIBLE
                        binding.btnSignup.isEnabled = false
                    }
                    is UiState.Success -> {
                        binding.pbLoading.visibility = View.GONE
                        Toast.makeText(this@SignupActivity, "회원가입 성공! 로그인해주세요.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    is UiState.Error -> {
                        binding.pbLoading.visibility = View.GONE
                        binding.btnSignup.isEnabled = true
                        Toast.makeText(this@SignupActivity, state.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }
    }
}
