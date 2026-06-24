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
    private var isFabExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tokenManager = TokenManager(requireContext())
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MenuViewModel(tokenManager) as T
            }
        }

        viewModel = ViewModelProvider(this, factory)[MenuViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
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
        updateCartBadge()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            updateCartBadge()
        }
    }

    private fun setupRecyclerView() {
        menuAdapter = MenuAdapter(emptyList()) { selectedMenu ->
            PersonalCartStore.add(selectedMenu)
            updateCartBadge()
            Toast.makeText(context, "${selectedMenu.name} 장바구니에 담았어요", Toast.LENGTH_SHORT).show()
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
                        is MenuUiState.Loading -> Unit
                        is MenuUiState.Success -> menuAdapter.updateList(state.menuList)
                        is MenuUiState.Error -> {
                            Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.fabPersonalCart.setOnClickListener {
            findNavController().navigate(R.id.action_fragment_order_to_fragment_cart)
        }

        binding.fabGroupMenu.setOnClickListener {
            toggleFab()
        }

        binding.fabCreateGroup.setOnClickListener {
            toggleFab()
            createGroupOrder()
        }

        binding.fabJoinGroup.setOnClickListener {
            toggleFab()
            showJoinGroupDialog()
        }
    }

    private fun showJoinGroupDialog() {
        val input = EditText(requireContext()).apply {
            hint = "초대 링크 또는 초대 토큰을 입력하세요"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("같이 주문 방 입장")
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
                                "방은 생성됐지만 로그인 사용자 정보를 찾을 수 없어요.",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@launch
                        }

                        Toast.makeText(
                            requireContext(),
                            "같이 주문 방을 만들었어요.",
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
                            "방 생성 응답이 비어 있어요.",
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

    private fun toggleFab() {
        if (isFabExpanded) {
            binding.fabCreateGroup.visibility = View.GONE
            binding.fabJoinGroup.visibility = View.GONE
            binding.fabGroupMenu.animate().rotation(0f).setDuration(180).start()
        } else {
            binding.fabCreateGroup.visibility = View.VISIBLE
            binding.fabJoinGroup.visibility = View.VISIBLE
            binding.fabGroupMenu.animate().rotation(45f).setDuration(180).start()
        }
        isFabExpanded = !isFabExpanded
    }

    private fun navigateToGroupOrder(
        groupId: String,
        isHost: Boolean,
        userId: Long,
        shareLink: String? = null
    ) {
        if (userId <= 0L) {
            Toast.makeText(requireContext(), "로그인 정보가 없어요. 다시 로그인해주세요.", Toast.LENGTH_SHORT).show()
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
                val response = groupOrderApiService.joinGroup(GroupJoinRequest(shareToken))

                if (response.isSuccessful) {
                    val body = response.body()

                    if (body != null) {
                        val myUserId = tokenManager.getUserId()

                        if (myUserId <= 0L) {
                            Toast.makeText(
                                requireContext(),
                                "로그인 정보가 없어요. 다시 로그인해주세요.",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@launch
                        }

                        Toast.makeText(
                            requireContext(),
                            "같이 주문 방에 입장했어요.",
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
                            "방 입장 응답이 비어 있어요.",
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

    private fun updateCartBadge() {
        val itemCount = PersonalCartStore.getItems().sumOf { it.quantity }

        binding.tvCartBadge.visibility = if (itemCount > 0) View.VISIBLE else View.GONE
        binding.tvCartBadge.text = if (itemCount > 99) "99+" else itemCount.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
