package com.ssafy.payclient.ui.signup

import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.ssafy.payclient.data.local.TokenManager
import com.ssafy.payclient.data.model.SignupRequest
import com.ssafy.payclient.data.network.RetrofitClient
import com.ssafy.payclient.data.repository.AuthRepository
import com.ssafy.payclient.databinding.ActivitySignupBinding
import com.ssafy.payclient.util.UiState
import com.ssafy.payclient.util.ViewModelFactory
import kotlinx.coroutines.launch
import kotlin.math.max

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

        setupKeyboardInsets()
        initViews()
        observeViewModel()
    }

    private fun initViews() {
        binding.btnSignupBack.setOnClickListener {
            finish()
        }

        binding.etSignupId.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                isIdChecked = false
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        binding.btnCheckId.setOnClickListener {
            val id = binding.etSignupId.text.toString()
            viewModel.checkId(id)
        }

        binding.btnSignup.setOnClickListener {
            if (!isIdChecked) {
                Toast.makeText(this, "아이디 중복 확인을 해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!binding.cbSignupTerms.isChecked) {
                Toast.makeText(this, "이용약관 및 개인정보처리방침에 동의해주세요.", Toast.LENGTH_SHORT).show()
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

        listOf(
            binding.etSignupId,
            binding.etSignupPassword,
            binding.etSignupPasswordConfirm,
            binding.etSignupNickname
        ).forEach { field ->
            field.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    binding.scrollSignup.postDelayed({ scrollFocusedFieldIntoView() }, 220)
                }
            }
        }
    }

    private fun setupKeyboardInsets() {
        val baseBottomPadding = binding.scrollSignup.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            val keyboardBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val bottomPadding = baseBottomPadding + max(systemBottom, keyboardBottom)

            binding.scrollSignup.setPadding(
                binding.scrollSignup.paddingLeft,
                binding.scrollSignup.paddingTop,
                binding.scrollSignup.paddingRight,
                bottomPadding
            )

            if (keyboardBottom > 0) {
                binding.scrollSignup.post { scrollFocusedFieldIntoView() }
            }

            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    private fun scrollFocusedFieldIntoView() {
        val focusedView = currentFocus ?: return
        val focusedRect = Rect()
        focusedView.getDrawingRect(focusedRect)
        binding.scrollSignup.offsetDescendantRectToMyCoords(focusedView, focusedRect)

        val visibleBottom = binding.scrollSignup.scrollY +
            binding.scrollSignup.height -
            binding.scrollSignup.paddingBottom
        val targetBottom = focusedRect.bottom + 36.dp()

        if (targetBottom > visibleBottom) {
            binding.scrollSignup.smoothScrollTo(
                0,
                binding.scrollSignup.scrollY + targetBottom - visibleBottom
            )
        }
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

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
