package com.ssafy.payclient.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
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
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDummySections()
        loadUserInfo()

        binding.btnLogout.setOnClickListener {
            viewModel.logout()
        }

        observeLogoutState()
    }

    private fun setupDummySections() {
        setOrderCard(
            containerId = R.id.profile_order_1,
            date = "2023.10.24 14:30",
            price = "9,500원",
            name = "시그니처 바닐라 라떼 외 1건",
            store = "디지털 취계 강남점"
        )
        setOrderCard(
            containerId = R.id.profile_order_2,
            date = "2023.10.22 09:15",
            price = "4,500원",
            name = "아이스 아메리카노",
            store = "디지털 취계 판교점"
        )
        setOrderCard(
            containerId = R.id.profile_order_3,
            date = "2023.10.20 18:45",
            price = "5,000원",
            name = "따뜻한 카페라떼",
            store = "디지털 취계 홍대점"
        )

        setMenuRow(R.id.profile_menu_favorite, R.drawable.ic_heart_outline, "자주 찾는 메뉴")
        setMenuRow(R.id.profile_menu_payment, R.drawable.ic_payment_card, "결제수단 관리")
        setMenuRow(R.id.profile_menu_alarm, R.drawable.ic_alarm_outline, "알림 설정")
    }

    private fun setOrderCard(
        containerId: Int,
        date: String,
        price: String,
        name: String,
        store: String
    ) {
        val container = binding.root.findViewById<View>(containerId) ?: return
        container.findViewById<TextView>(R.id.tv_order_date)?.text = date
        container.findViewById<TextView>(R.id.tv_order_price)?.text = price
        container.findViewById<TextView>(R.id.tv_order_name)?.text = name
        container.findViewById<TextView>(R.id.tv_order_store)?.text = store
    }

    private fun setMenuRow(containerId: Int, iconRes: Int, title: String) {
        val container = binding.root.findViewById<View>(containerId) ?: return
        container.findViewById<ImageView>(R.id.iv_menu_icon)?.setImageResource(iconRes)
        container.findViewById<TextView>(R.id.tv_menu_title)?.text = title
    }

    private fun loadUserInfo() {
        val tokenManager = TokenManager(requireContext().applicationContext)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val apiService = RetrofitClient.getUserApiService(tokenManager)
                val userResponse = apiService.getUserInfo()

                binding.tvNickname.text = userResponse.nickname
                binding.tvEmail.text = userResponse.loginId
                com.bumptech.glide.Glide.with(binding.root.context)
                    .load(userResponse.imageUrl)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .into(binding.ivProfile)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "사용자 정보를 불러오지 못했어요: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeLogoutState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.logoutState.collectLatest { state ->
                when (state) {
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
                    else -> Unit
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
