package com.ssafy.payclient.fragment

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.database.*
import com.ssafy.payclient.R
import com.ssafy.payclient.data.local.TokenManager
import com.ssafy.payclient.data.model.FirebaseCartItem
import com.ssafy.payclient.databinding.FragmentGroupOrderBinding
import com.ssafy.payclient.ui.menu.MenuAdapter
import com.ssafy.payclient.ui.menu.MenuUiState
import com.ssafy.payclient.ui.menu.MenuViewModel
import kotlinx.coroutines.launch

class GroupOrderFragment : Fragment() {

    private var _binding: FragmentGroupOrderBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MenuViewModel
    private lateinit var menuAdapter: MenuAdapter

    private lateinit var database: DatabaseReference
    private var groupId: String = ""
    private var isHost: Boolean = false
    private var currentUserId: Long = -1L

    private var shareLink: String? = null

    private var groupStatus: String = "OPEN"
    private var groupStatusListener: ValueEventListener? = null
    private var cartItemsListener: ValueEventListener? = null
    private var isHeaderExpanded: Boolean = false

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
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupOrderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        database = FirebaseDatabase.getInstance("https://pickpay-be337-default-rtdb.firebaseio.com/").reference

        groupId = arguments?.getString("GROUP_ID").orEmpty()
        isHost = arguments?.getBoolean("IS_HOST") ?: false
        currentUserId = arguments?.getLong("USER_ID") ?: -1L
        shareLink = arguments?.getString("SHARE_LINK")

        renderGroupHeader()

        setupToolbar()
        setupRecyclerView()
        setupFabs()
        observeViewModel()
        observeGroupStatus()
        observeCartBadge()

    }

    private fun observeGroupStatus() {
        groupStatusListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val status = snapshot.getValue(String::class.java) ?: "OPEN"
                groupStatus = status
                renderGroupHeader()
            }

            override fun onCancelled(error: DatabaseError) {
                if (_binding == null) return
                Toast.makeText(
                    requireContext(),
                    "방 상태 확인 실패: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        getGroupStatusRef()
            .addValueEventListener(groupStatusListener!!)
    }

    private fun observeCartBadge() {
        cartItemsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val binding = _binding ?: return
                val itemCount = snapshot.children.sumOf { itemSnapshot ->
                    itemSnapshot.getValue(FirebaseCartItem::class.java)?.quantity ?: 0
                }
                binding.tvGroupCartBadge.text = itemCount.toString()
                binding.tvGroupCartBadge.visibility = if (itemCount > 0) View.VISIBLE else View.GONE
            }

            override fun onCancelled(error: DatabaseError) = Unit
        }

        getGroupItemsRef().addValueEventListener(cartItemsListener!!)
    }

    private fun getGroupStatusRef(): DatabaseReference {
        return database
            .child("group_orders")
            .child(groupId)
            .child("status")
    }

    private fun getGroupItemsRef(): DatabaseReference {
        return database
            .child("group_orders")
            .child(groupId)
            .child("items")
    }

    private fun setupToolbar() {
        binding.toolbarGroupOrder.navigationIcon?.setTint(
            ContextCompat.getColor(requireContext(), R.color.white)
        )
        binding.toolbarGroupOrder.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        binding.btnToggleHeader.setOnClickListener {
            isHeaderExpanded = !isHeaderExpanded
            renderHeaderExpansion(animate = true)
        }
        renderHeaderExpansion(animate = false)
    }

    private fun renderGroupHeader() {
        val binding = _binding ?: return
        binding.tvGroupIdChip.text = "ID: ${groupId.shortRoomId()}"
        binding.tvGroupHostChip.text = if (isHost) "방장" else "참여"
        binding.tvGroupStatusChip.text = buildStatusChipText()
        binding.tvGroupStatus.text = "메뉴를 담으면 실시간으로 공유됩니다."
    }

    private fun buildStatusChipText(): SpannableString {
        val statusText = if (groupStatus == "OPEN") "● OPEN" else "● $groupStatus"
        return SpannableString(statusText).apply {
            val dotColor = if (groupStatus == "OPEN") R.color.open_green else R.color.white
            setSpan(
                ForegroundColorSpan(ContextCompat.getColor(requireContext(), dotColor)),
                0,
                1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun renderHeaderExpansion(animate: Boolean) {
        val binding = _binding ?: return
        val targetHeight = if (isHeaderExpanded) 204.dp() else 104.dp()

        binding.layoutGroupChips.visibility = if (isHeaderExpanded) View.VISIBLE else View.GONE
        binding.tvGroupStatus.visibility = if (isHeaderExpanded) View.VISIBLE else View.GONE
        binding.btnToggleHeader.rotation = if (isHeaderExpanded) 0f else 180f
        binding.btnToggleHeader.contentDescription =
            if (isHeaderExpanded) "공동 주문방 정보 접기" else "공동 주문방 정보 펼치기"

        val params = binding.layoutGroupOrderHeader.layoutParams
        if (!animate) {
            params.height = targetHeight
            binding.layoutGroupOrderHeader.layoutParams = params
            return
        }

        ValueAnimator.ofInt(params.height, targetHeight).apply {
            duration = 180L
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                params.height = animator.animatedValue as Int
                binding.layoutGroupOrderHeader.layoutParams = params
            }
            start()
        }
    }

    private fun String.shortRoomId(): String {
        if (isBlank()) return "-"
        return if (length > 8) "${take(8)}..." else this
    }

    private fun Int.dp(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    private fun setupRecyclerView() {
        menuAdapter = MenuAdapter(emptyList()) { selectedMenu ->
            addItemToFirebaseCart(selectedMenu.name, selectedMenu.menuId, 1)
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
                                addItemToFirebaseCart(selectedMenu.name, selectedMenu.menuId, 1)
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
        binding.tvGroupCartBadge.bringToFront()
        binding.fabShareLink.setOnClickListener {
            val link = shareLink

            if (link.isNullOrBlank()) {
                Toast.makeText(
                    requireContext(),
                    "공유 링크가 없습니다.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(
                    Intent.EXTRA_TEXT,
                    "픽페이 함께 주문에 초대합니다!\n$link"
                )
                type = "text/plain"
            }

            startActivity(Intent.createChooser(shareIntent, "초대 링크 공유"))
        }

        binding.fabChat.setOnClickListener {
            val bundle = Bundle().apply {
                putString("GROUP_ID", groupId)
                putBoolean("IS_HOST", isHost)
                putLong("USER_ID", currentUserId)
            }
            findNavController().navigate(R.id.action_fragment_group_order_to_fragment_group_chat, bundle)
        }
        binding.fabGroupCart.setOnClickListener {
            val bundle = Bundle().apply {
                putString("GROUP_ID", groupId)
                putBoolean("IS_HOST", isHost)
                putLong("USER_ID", currentUserId)
            }
            findNavController().navigate(R.id.action_fragment_group_order_to_fragment_group_cart, bundle)
        }
    }

    private fun addItemToFirebaseCart(menuName: String, productId: Long, quantity: Int) {

        // LOCKED or PAID 상태면 장바구니 담기 X
        if (groupStatus != "OPEN") {
            Toast.makeText(
                requireContext(),
                "주문이 마감되어 더 이상 메뉴를 담을 수 없습니다.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val itemKey = "user${currentUserId}_item_${productId}"

        val itemRef = getGroupItemsRef().child(itemKey)

        itemRef.runTransaction(object : Transaction.Handler {

            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val currentItem = currentData.getValue(FirebaseCartItem::class.java)

                if (currentItem == null) {
                    currentData.value = FirebaseCartItem(
                        menuName = menuName,
                        productId = productId,
                        quantity = quantity,
                        userId = currentUserId
                    )
                } else {
                    currentData.value = currentItem.copy(
                        productId = currentItem.productId.takeIf { it > 0L } ?: productId,
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
        groupStatusListener?.let {
            getGroupStatusRef().removeEventListener(it)
        }
        cartItemsListener?.let {
            getGroupItemsRef().removeEventListener(it)
        }
        groupStatusListener = null
        cartItemsListener = null
        _binding = null
    }
}
