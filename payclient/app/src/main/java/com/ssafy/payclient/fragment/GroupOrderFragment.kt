package com.ssafy.payclient.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.database.*
import com.ssafy.payclient.R
import com.ssafy.payclient.data.model.CartItem
import com.ssafy.payclient.databinding.FragmentGroupOrderBinding
import com.ssafy.payclient.ui.menu.MenuAdapter
import com.ssafy.payclient.ui.menu.MenuUiState
import com.ssafy.payclient.ui.menu.MenuViewModel
import kotlinx.coroutines.launch

class GroupOrderFragment : Fragment() {

    private var _binding: FragmentGroupOrderBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MenuViewModel by viewModels()
    private lateinit var menuAdapter: MenuAdapter

    private lateinit var database: DatabaseReference
    private var groupId: Long = -1L
    private var isHost: Boolean = false
    private var currentUserId: Long = -1L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupOrderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        database = FirebaseDatabase.getInstance("https://pickpay-be337-default-rtdb.firebaseio.com/").reference

        groupId = arguments?.getLong("GROUP_ID") ?: -1L
        isHost = arguments?.getBoolean("IS_HOST") ?: false
        currentUserId = arguments?.getLong("USER_ID") ?: -1L

        binding.tvGroupStatus.text = "현재 방 번호: $groupId | 방장 여부: $isHost\n메뉴를 담으면 실시간으로 공유됩니다."

        setupToolbar()
        setupRecyclerView()
        setupFabs()
        observeViewModel()
    }

    private fun setupToolbar() {
        // 상단바의 뒤로가기 아이콘 클릭 시 이전 화면으로 이동
        binding.toolbarGroupOrder.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupRecyclerView() {
        menuAdapter = MenuAdapter(emptyList()) { selectedMenu ->
            addItemToFirebaseCart(selectedMenu.menuName, selectedMenu.menuId, 1)
        }
        binding.rvGroupMenuList.apply {
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
                                addItemToFirebaseCart(selectedMenu.menuName, selectedMenu.menuId, 1)
                            }
                            binding.rvGroupMenuList.adapter = menuAdapter
                        }
                        is MenuUiState.Error -> {
                            Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun setupFabs() {
        binding.fabShareLink.setOnClickListener {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "픽페이 함께 주문에 초대합니다!\n초대 코드: $groupId")
                type = "text/plain"
            }
            startActivity(Intent.createChooser(shareIntent, "초대 링크 공유"))
        }

        binding.fabChat.setOnClickListener {
            Toast.makeText(context, "단체 채팅 & 룰렛 화면으로 이동 (추후 구현)", Toast.LENGTH_SHORT).show()
        }

        binding.fabGroupCart.setOnClickListener {
            val bundle = Bundle().apply {
                putLong("GROUP_ID", groupId)
                putBoolean("IS_HOST", isHost)
                putLong("USER_ID", currentUserId)
            }
            findNavController().navigate(R.id.action_fragment_group_order_to_fragment_group_cart, bundle)
        }
    }

    private fun addItemToFirebaseCart(menuName: String, menuId: Long, quantity: Int) {
        val itemKey = "user${currentUserId}_item_${menuId}"

        val itemRef = database
            .child("group_orders")
            .child(groupId.toString())
            .child("items")
            .child(itemKey)

        itemRef.runTransaction(object : Transaction.Handler {

            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val currentItem = currentData.getValue(CartItem::class.java)

                if (currentItem == null) {
                    currentData.value = CartItem(
                        menuName = menuName,
                        menuId = menuId,
                        quantity = quantity,
                        userId = currentUserId
                    )
                } else {
                    currentData.value = currentItem.copy(
                        quantity = currentItem.quantity + quantity
                    )
                }

                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                currentData: DataSnapshot?
            ) {
                if (committed) {
                    Toast.makeText(context, "$menuName 담기 성공", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "담기 실패: ${error?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}