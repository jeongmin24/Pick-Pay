package com.ssafy.payclient.fragment

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.ssafy.payclient.R
import com.ssafy.payclient.data.local.PersonalCartStore
import com.ssafy.payclient.data.local.TokenManager
import com.ssafy.payclient.data.model.GroupJoinRequest
import com.ssafy.payclient.data.network.RetrofitClient
import com.ssafy.payclient.databinding.FragmentOrderBinding
import com.ssafy.payclient.ui.menu.MenuAdapter
import com.ssafy.payclient.ui.menu.MenuUiState
import com.ssafy.payclient.ui.menu.MenuViewModel
import kotlinx.coroutines.launch

class OrderFragment : Fragment() {

    private var _binding: FragmentOrderBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MenuViewModel
    private lateinit var menuAdapter: MenuAdapter

    // FAB 메뉴 확장 여부를 확인하는 플래그
    private var isFabExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tokenManager = TokenManager(requireContext())
        val factory = object  : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MenuViewModel(tokenManager) as T
            }
        }

        viewModel = ViewModelProvider(this, factory)[MenuViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        menuAdapter = MenuAdapter(emptyList()) { selectedMenu ->
            PersonalCartStore.add(selectedMenu)
            Toast.makeText(context, "${selectedMenu.name} (개인 장바구니에 담김)", Toast.LENGTH_SHORT).show()
        }
        binding.rvMenuList.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = menuAdapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.menuState.collect { state ->
                    when (state) {
                        is MenuUiState.Loading -> {}
                        is MenuUiState.Success -> {
                            menuAdapter.updateList(state.menuList)
                        }
                        is MenuUiState.Error -> {
                            Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        // 1. 개인 카트 버튼
        binding.fabPersonalCart.setOnClickListener {
            findNavController().navigate(R.id.action_fragment_order_to_fragment_cart)
        }

        // 2. 메인 FAB (더보기) 클릭 시 확장/축소 토글
        binding.fabGroupMenu.setOnClickListener {
            toggleFab()
        }

        // 4. 방 생성 버튼
        binding.fabCreateGroup.setOnClickListener {
            toggleFab() // 메뉴 먼저 닫기
            createGroupOrder()
        }

        // 5. 방 입장 버튼
        binding.fabJoinGroup.setOnClickListener {
            toggleFab() // 메뉴 먼저 닫기
            val input = EditText(requireContext())
            input.hint = "초대 링크 또는 초대 토큰을 입력하세요"

            AlertDialog.Builder(requireContext())
                .setTitle("방 입장하기")
                .setView(input)
                .setPositiveButton("입장") { _, _ ->
                    val inputText = input.text.toString()

                    if (inputText.isNotBlank()) {
                        joinGroupOrder(inputText)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "초대 링크 또는 토큰을 입력해주세요.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .setNegativeButton("취소", null)
                .show()
        }
    }

    private fun createGroupOrder() {
        val tokenManager = TokenManager(requireContext())
        val groupOrderApiService = RetrofitClient.getGroupOrderApiService(tokenManager)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = groupOrderApiService.createGroupOrder()

                if (response.isSuccessful) {
                    val body = response.body()

                    if (body != null) {
                        val myUserId = tokenManager.getUserId()

                        if (myUserId <= 0L) {
                            Toast.makeText(
                                requireContext(),
                                "방 생성은 성공했지만 userId가 없습니다.",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@launch
                        }

                        Toast.makeText(
                            requireContext(),
                            "방 생성 성공: ${body.groupId}",
                            Toast.LENGTH_SHORT
                        ).show()

                        navigateToGroupOrder(
                            groupId = body.groupId,
                            isHost = true,
                            userId = myUserId,
                            shareLink = body.shareLink
                        )
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "방 생성 응답이 비어 있습니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "방 생성 실패: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "방 생성 오류: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // 플로팅 버튼(FAB) 애니메이션 및 표시 상태 변경 로직
    private fun toggleFab() {
        if (isFabExpanded) {
            // 축소하기 (닫기)
            binding.fabCreateGroup.visibility = View.GONE
            binding.fabJoinGroup.visibility = View.GONE
            binding.fabGroupMenu.animate().rotation(0f).setDuration(200).start()
        } else {
            // 확장하기 (열기)
            binding.fabCreateGroup.visibility = View.VISIBLE
            binding.fabJoinGroup.visibility = View.VISIBLE
            // 아이콘을 90도 회전시켜 활성화된 느낌을 줍니다.
            binding.fabGroupMenu.animate().rotation(90f).setDuration(200).start()
        }
        isFabExpanded = !isFabExpanded
    }

    private fun navigateToGroupOrder(groupId: String, isHost: Boolean, userId: Long, shareLink: String?=null) {
        val tokenManager = TokenManager(requireContext())

        if (userId <= 0L) {
            Toast.makeText(requireContext(), "로그인 정보가 없습니다. 다시 로그인해주세요.: $userId", Toast.LENGTH_SHORT).show()
            return
        }

        val bundle = Bundle().apply {
            putString("GROUP_ID", groupId)
            putBoolean("IS_HOST", isHost)
            putLong("USER_ID", userId)
            putString("SHARE_LINK", shareLink)
        }

        findNavController().navigate(R.id.action_fragment_order_to_fragment_group_order, bundle)
    }

    private fun joinGroupOrder(inputText: String) {
        val shareToken = extractShareToken(inputText)

        if (shareToken.isBlank()) {
            Toast.makeText(
                requireContext(),
                "초대 링크 또는 토큰을 입력해주세요.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val tokenManager = TokenManager(requireContext())
        val groupOrderApiService = RetrofitClient.getGroupOrderApiService(tokenManager)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = groupOrderApiService.joinGroup(
                    GroupJoinRequest(shareToken)
                )

                if (response.isSuccessful) {
                    val body = response.body()

                    if (body != null) {
                        val myUserId = tokenManager.getUserId()

                        if (myUserId <= 0L) {
                            Toast.makeText(
                                requireContext(),
                                "userId가 없습니다. 다시 로그인해주세요.",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@launch
                        }

                        Toast.makeText(
                            requireContext(),
                            "방 입장 성공: ${body.groupId}",
                            Toast.LENGTH_SHORT
                        ).show()

                        val shareLink = "pickpay://group/join?token=$shareToken"

                        navigateToGroupOrder(
                            groupId = body.groupId,
                            isHost = body.host,
                            userId = myUserId,
                            shareLink = shareLink
                        )
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "방 입장 응답이 비어 있습니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "방 입장 실패: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "방 입장 오류: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun extractShareToken(input: String): String {
        val trimmed = input.trim()

        return if (trimmed.contains("token=")) {
            Uri.parse(trimmed).getQueryParameter("token") ?: ""
        } else {
            trimmed
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
