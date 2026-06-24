package com.ssafy.payclient.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.ssafy.payclient.BuildConfig
import com.ssafy.payclient.MainViewModel
import com.ssafy.payclient.R
import com.ssafy.payclient.data.local.TokenManager
import com.ssafy.payclient.data.network.RetrofitClient
import com.ssafy.payclient.data.repository.AuthRepository
import com.ssafy.payclient.databinding.FragmentProfileBinding
import com.ssafy.payclient.ui.login.LoginActivity
import com.ssafy.payclient.ui.profile.RecentOrderAdapter
import com.ssafy.payclient.util.UiState
import com.ssafy.payclient.util.ViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.getValue

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var recentOrderAdapter: RecentOrderAdapter

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

        setupRecentOrders()
        setupStaticMenuRows()
        loadUserInfo()
        loadRecentOrders()

        binding.btnLogout.setOnClickListener {
            viewModel.logout()
        }

        observeLogoutState()
    }

    private fun setupRecentOrders() {
        recentOrderAdapter = RecentOrderAdapter()
        binding.rvOrderHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recentOrderAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupStaticMenuRows() {
        setProfileMenuRow(R.id.profile_menu_favorite, R.drawable.ic_heart_outline, "자주 찾는 메뉴")
        setProfileMenuRow(R.id.profile_menu_payment, R.drawable.ic_payment_card, "결제수단 관리")
        setProfileMenuRow(R.id.profile_menu_alarm, R.drawable.ic_alarm_outline, "알림 설정")
    }

    private fun setProfileMenuRow(containerId: Int, iconRes: Int, title: String) {
        val container = binding.root.findViewById<View>(containerId) ?: return
        container.findViewById<android.widget.ImageView>(R.id.iv_menu_icon)?.setImageResource(iconRes)
        container.findViewById<android.widget.TextView>(R.id.tv_menu_title)?.text = title
    }

    private fun loadUserInfo() {
        val tokenManager = TokenManager(requireContext().applicationContext)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val apiService = RetrofitClient.getUserApiService(tokenManager)
                val userResponse = apiService.getUserInfo()

                binding.tvNickname.text = userResponse.nickname
                binding.tvEmail.text = userResponse.loginId
                Glide.with(binding.root.context)
                    .load(resolveProfileImageUrl(userResponse.imageUrl))
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .circleCrop()
                    .into(binding.ivProfile)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "유저 정보 오류: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resolveProfileImageUrl(imageUrl: String?): String? {
        val rawUrl = imageUrl?.trim().orEmpty()
        if (rawUrl.isBlank()) return null

        if (
            rawUrl.startsWith("http://") ||
            rawUrl.startsWith("https://") ||
            rawUrl.startsWith("content://") ||
            rawUrl.startsWith("file://")
        ) {
            return rawUrl
        }

        val baseUrl = BuildConfig.BASE_URL.trimEnd('/')
        val baseUri = Uri.parse(baseUrl)
        val serverRoot = if (!baseUri.scheme.isNullOrBlank() && !baseUri.authority.isNullOrBlank()) {
            "${baseUri.scheme}://${baseUri.authority}"
        } else {
            baseUrl
        }

        return if (rawUrl.startsWith("/")) {
            serverRoot + rawUrl
        } else {
            "$serverRoot/$rawUrl"
        }
    }

    private fun loadRecentOrders() {
        val tokenManager = TokenManager(requireContext().applicationContext)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val apiService = RetrofitClient.getIndividualOrderApiService(tokenManager)
                val response = apiService.getRecentOrders(limit = 10)

                if (response.isSuccessful) {
                    val orders = response.body().orEmpty()
                    recentOrderAdapter.submitList(orders)
                    binding.rvOrderHistory.visibility =
                        if (orders.isEmpty()) View.GONE else View.VISIBLE
                    binding.tvOrderHistoryEmpty.visibility =
                        if (orders.isEmpty()) View.VISIBLE else View.GONE
                } else {
                    binding.rvOrderHistory.visibility = View.GONE
                    binding.tvOrderHistoryEmpty.visibility = View.VISIBLE
                    Toast.makeText(
                        requireContext(),
                        "최근 주문을 불러오지 못했어요. (${response.code()})",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                binding.rvOrderHistory.visibility = View.GONE
                binding.tvOrderHistoryEmpty.visibility = View.VISIBLE
                Toast.makeText(
                    requireContext(),
                    "최근 주문 오류: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
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
