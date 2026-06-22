package com.ssafy.payclient.fragment

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.ssafy.payclient.MainViewModel
import com.ssafy.payclient.R
import com.ssafy.payclient.data.local.TokenManager
import com.ssafy.payclient.data.network.RetrofitClient
import com.ssafy.payclient.data.repository.AuthRepository
import com.ssafy.payclient.databinding.FragmentProfileBinding
import com.ssafy.payclient.ui.login.LoginActivity
import com.ssafy.payclient.util.UiState
import com.ssafy.payclient.util.ViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.getValue

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels {
        val tokenManager = TokenManager(requireContext().applicationContext)
        val apiService = RetrofitClient.getApiService(tokenManager)
        val repository = AuthRepository(apiService)
        ViewModelFactory(repository, tokenManager)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnLogout.setOnClickListener {
            viewModel.logout()
        }

        observeLogoutState()
    }

    private fun observeLogoutState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.logoutState.collectLatest { state ->
                when(state) {
                    is UiState.Loading -> binding.btnLogout.isEnabled = false
                    is UiState.Success -> {
                        binding.btnLogout.isEnabled = true
                        Toast.makeText(requireContext(), "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()
                        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        activity?.finish()
                    }
                    is UiState.Error -> {
                        binding.btnLogout.isEnabled = true
                        Toast.makeText(requireContext(), "오류: ${state.message}", Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}