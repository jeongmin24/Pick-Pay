package com.ssafy.payclient.fragment

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
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.ssafy.payclient.R
import com.ssafy.payclient.data.local.TokenManager
import com.ssafy.payclient.data.network.RetrofitClient
import com.ssafy.payclient.databinding.FragmentOrderBinding
import com.ssafy.payclient.ui.menu.MenuAdapter
import com.ssafy.payclient.ui.menu.MenuUiState
import com.ssafy.payclient.ui.menu.MenuViewModel
import kotlinx.coroutines.launch

class OrderFragment : Fragment() {

    private var _binding: FragmentOrderBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MenuViewModel by viewModels()
    private lateinit var menuAdapter: MenuAdapter

    // FAB 메뉴 확장 여부를 확인하는 플래그
    private var isFabExpanded = false

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
            Toast.makeText(context, "${selectedMenu.menuName} (개인 장바구니에 담김)", Toast.LENGTH_SHORT).show()
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
                            menuAdapter = MenuAdapter(state.menuList) { selectedMenu ->
                                Toast.makeText(context, "${selectedMenu.menuName} (개인 장바구니에 담김)", Toast.LENGTH_SHORT).show()
                            }
                            binding.rvMenuList.adapter = menuAdapter
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
            Toast.makeText(context, "개인 장바구니 화면으로 이동", Toast.LENGTH_SHORT).show()
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
            input.hint = "입장할 방 번호를 입력하세요"
            AlertDialog.Builder(requireContext())
                .setTitle("방 입장하기")
                .setView(input)
                .setPositiveButton("입장") { _, _ ->
                    val roomIdStr = input.text.toString()
                    if (roomIdStr.isNotEmpty()) {
                        navigateToGroupOrder(roomIdStr.toLong(), isHost = false)
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
                        Toast.makeText(
                            requireContext(),
                            "방 생성 성공: ${body.groupId}",
                            Toast.LENGTH_SHORT
                        ).show()

                        navigateToGroupOrder(
                            groupId = body.groupId,
                            isHost = true
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

    private fun navigateToGroupOrder(groupId: Long, isHost: Boolean) {
        val tokenManager = TokenManager(requireContext())
        val myUserId = tokenManager.getUserId()

        if (myUserId <= 0L) {
            Toast.makeText(requireContext(), "로그인 정보가 없습니다. 다시 로그인해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val bundle = Bundle().apply {
            putLong("GROUP_ID", groupId)
            putBoolean("IS_HOST", isHost)
            putLong("USER_ID", myUserId)
        }
        findNavController().navigate(R.id.action_fragment_order_to_fragment_group_order, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}