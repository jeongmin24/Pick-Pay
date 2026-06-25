package com.ssafy.payclient.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.ssafy.payclient.R
import com.ssafy.payclient.data.model.FirebaseCartItem
import com.ssafy.payclient.databinding.FragmentGroupCartBinding
import com.ssafy.payclient.ui.cart.GroupCartAdapter
import com.ssafy.payclient.ui.cart.GroupCartItemUi
import java.text.NumberFormat
import java.util.Locale

class GroupCartFragment : Fragment() {

    private var _binding: FragmentGroupCartBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: DatabaseReference
    private lateinit var groupCartAdapter: GroupCartAdapter

    private var groupId: String = ""
    private var isHost: Boolean = false
    private var currentUserId: Long = -1L
    private var cartItemsListener: ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        groupId = arguments?.getString("GROUP_ID").orEmpty()
        isHost = arguments?.getBoolean("IS_HOST") ?: false
        currentUserId = arguments?.getLong("USER_ID") ?: -1L

        database = FirebaseDatabase.getInstance("https://pickpay-be337-default-rtdb.firebaseio.com/").reference

        setupToolbar()
        setupSystemBarInsets()
        setupRecyclerView()
        setupCloseButton()
        loadCartData()
    }

    private fun setupToolbar() {
        binding.toolbarGroupCart.navigationIcon?.setTint(
            ContextCompat.getColor(requireContext(), R.color.coffee_brown_dark)
        )
        binding.toolbarGroupCart.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupSystemBarInsets() {
        val summaryLayout = binding.layoutGroupOrderSummary
        val baseBottomPadding = summaryLayout.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            summaryLayout.updatePadding(bottom = baseBottomPadding + bottomInset)
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    private fun setupRecyclerView() {
        groupCartAdapter = GroupCartAdapter(
            cartItems = emptyList(),
            currentUserId = currentUserId,
            onIncreaseClick = { cartItem ->
                updateMyCartItemQuantity(cartItem, 1)
            },
            onDecreaseClick = { cartItem ->
                updateMyCartItemQuantity(cartItem, -1)
            },
            onRemoveClick = { cartItem ->
                removeMyCartItem(cartItem)
            }
        )

        binding.rvGroupCartItems.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = groupCartAdapter
        }
    }

    private fun setupCloseButton() {
        binding.btnCloseOrder.visibility = View.VISIBLE
        binding.btnCloseOrder.setOnClickListener {
            closeOrderAndProceedToPayment()
        }
    }

    private fun loadCartData() {
        val itemsRef = getItemsRef()

        cartItemsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = snapshot.children.mapNotNull { itemSnapshot ->
                    val itemKey = itemSnapshot.key ?: return@mapNotNull null
                    val item = itemSnapshot.toFirebaseCartItem() ?: return@mapNotNull null
                    if (item.quantity <= 0) return@mapNotNull null
                    GroupCartItemUi(itemKey, item)
                }.sortedWith(compareBy<GroupCartItemUi> { it.item.userId }.thenBy { it.item.menuName })

                renderCart(items)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load cart: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        itemsRef.addValueEventListener(cartItemsListener!!)
    }

    private fun renderCart(items: List<GroupCartItemUi>) {
        groupCartAdapter.updateItems(items)

        val isEmpty = items.isEmpty()
        val itemCount = items.sumOf { it.item.quantity }
        val totalPrice = items.sumOf { it.item.price * it.item.quantity }

        binding.tvGroupCartSummary.text = "총 ${itemCount}개의 메뉴가 담겨있습니다."
        binding.tvGroupCartTotal.text = formatWon(totalPrice)
        binding.tvEmptyGroupCart.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvGroupCartItems.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.btnCloseOrder.isEnabled = isHost && !isEmpty
        binding.btnCloseOrder.text = if (isHost) {
            if (isEmpty) "메뉴를 담아주세요" else "주문 마감하기"
        } else {
            "방장이 주문을 마감합니다"
        }
    }

    private fun updateMyCartItemQuantity(cartItem: GroupCartItemUi, delta: Int) {
        if (cartItem.item.userId != currentUserId) return

        getItemsRef().child(cartItem.itemKey).runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val currentItem = currentData.getValue(FirebaseCartItem::class.java)
                    ?: return Transaction.success(currentData)
                val nextQuantity = currentItem.quantity + delta

                if (nextQuantity <= 0) {
                    currentData.value = null
                } else {
                    val productId = currentItem.productId.takeIf { it > 0L }
                        ?: currentData.child("menuId").getValue(Long::class.java)
                        ?: cartItem.item.productId
                    currentData.value = currentItem.copy(
                        productId = productId,
                        quantity = nextQuantity
                    )
                }

                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                currentData: DataSnapshot?
            ) {
                if (error != null) {
                    Toast.makeText(requireContext(), "Failed to update item: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun removeMyCartItem(cartItem: GroupCartItemUi) {
        if (cartItem.item.userId != currentUserId) return

        getItemsRef().child(cartItem.itemKey).removeValue()
            .addOnFailureListener { error ->
                Toast.makeText(requireContext(), "Failed to remove item: ${error.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getItemsRef(): DatabaseReference {
        return database.child("group_orders").child(groupId).child("items")
    }

    private fun DataSnapshot.toFirebaseCartItem(): FirebaseCartItem? {
        val item = getValue(FirebaseCartItem::class.java) ?: return null
        val productId = item.productId.takeIf { it > 0L }
            ?: child("menuId").getValue(Long::class.java)
            ?: return item
        return item.copy(productId = productId)
    }

    private fun formatWon(value: Long): String = "₩${PRICE_FORMAT.format(value)}"

    private fun closeOrderAndProceedToPayment() {
        if (groupId.isBlank()) {
            Toast.makeText(requireContext(), "그룹방 정보를 확인할 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        findNavController().navigate(
            R.id.action_fragment_group_cart_to_fragment_group_pay_type,
            Bundle().apply {
                putString("GROUP_ID", groupId)
                putBoolean("IS_HOST", isHost)
                putLong("USER_ID", currentUserId)
            }
        )
    }

    override fun onResume() {
        super.onResume()
        activity?.findViewById<View>(R.id.bottom_navigation)?.visibility = View.GONE
    }

    override fun onStop() {
        super.onStop()
        activity?.findViewById<View>(R.id.bottom_navigation)?.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cartItemsListener?.let {
            getItemsRef().removeEventListener(it)
        }
        _binding = null
    }

    companion object {
        private val PRICE_FORMAT: NumberFormat = NumberFormat.getNumberInstance(Locale.KOREA)
    }
}
