package com.ssafy.payclient.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ssafy.payclient.R
import com.ssafy.payclient.data.local.PersonalCartItem
import com.ssafy.payclient.data.local.PersonalCartStore
import com.ssafy.payclient.data.local.TokenManager
import com.ssafy.payclient.data.model.CartItemRequest
import com.ssafy.payclient.data.model.IndividualOrderRequestDTO
import com.ssafy.payclient.data.network.RetrofitClient
import com.ssafy.payclient.databinding.ActivityCartBinding
import com.ssafy.payclient.ui.cart.CartAdapter
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class CartFragment : Fragment() {

    private var _binding: ActivityCartBinding? = null
    private val binding get() = _binding!!

    private lateinit var cartAdapter: CartAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupHeader()
        setupSystemBarInsets()
        setupRecyclerView()
        setupClickListeners()
        renderCart()
    }

    private fun setupHeader() {
        binding.btnCartBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupSystemBarInsets() {
        val summaryLayout = binding.layoutOrderSummary
        val baseBottomPadding = summaryLayout.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            summaryLayout.updatePadding(bottom = baseBottomPadding + bottomInset)
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    private fun setupRecyclerView() {
        cartAdapter = CartAdapter(
            cartItems = emptyList(),
            onIncreaseClick = { item ->
                PersonalCartStore.increase(item.menuId)
                renderCart()
            },
            onDecreaseClick = { item ->
                PersonalCartStore.decrease(item.menuId)
                renderCart()
            },
            onRemoveClick = { item ->
                PersonalCartStore.remove(item.menuId)
                renderCart()
            }
        )

        binding.rvCartItems.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = cartAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnOrder.setOnClickListener {
            createOrder()
        }
    }

    private fun createOrder() {
        val cartItems = PersonalCartStore.getItems()
        if (cartItems.isEmpty()) {
            Toast.makeText(requireContext(), "장바구니가 비어있어요.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnOrder.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val tokenManager = TokenManager(requireContext().applicationContext)
                val apiService = RetrofitClient.getIndividualOrderApiService(tokenManager)
                val request = IndividualOrderRequestDTO(
                    items = cartItems.map { item ->
                        CartItemRequest(
                            menuId = item.menuId,
                            quantity = item.quantity
                        )
                    }
                )
                val response = apiService.createOrder(request)

                if (response.isSuccessful) {
                    val order = response.body()
                    if (order == null) {
                        Toast.makeText(requireContext(), "주문 응답이 비어 있어요.", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    findNavController().navigate(
                        R.id.action_fragment_cart_to_fragment_payment,
                        Bundle().apply {
                            putString(PaymentFragment.ARG_ORDER_ID, order.orderId)
                            putLong(PaymentFragment.ARG_TOTAL_PRICE, order.totalPrice)
                            putString(PaymentFragment.ARG_ORDER_NAME, buildOrderName(cartItems))
                        }
                    )
                } else {
                    Toast.makeText(requireContext(), "주문 생성에 실패했어요. (${response.code()})", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "주문 생성 중 오류가 발생했어요: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                _binding?.btnOrder?.isEnabled = PersonalCartStore.getItems().isNotEmpty()
            }
        }
    }

    private fun buildOrderName(items: List<PersonalCartItem>): String {
        val firstItemName = items.firstOrNull()?.menuName ?: "주문"
        return if (items.size == 1) firstItemName else "$firstItemName 외 ${items.size - 1}건"
    }

    private fun renderCart() {
        val items = PersonalCartStore.getItems()
        cartAdapter.updateItems(items)

        val itemCount = items.sumOf { it.quantity }
        val isEmpty = items.isEmpty()
        binding.tvCartSummary.text = "총 ${itemCount}개의 메뉴가 담겨있습니다."
        binding.tvEmptyCart.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvCartItems.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.btnOrder.isEnabled = !isEmpty
        binding.tvTotalPrice.text = formatWon(PersonalCartStore.getTotalPrice())
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
        _binding = null
    }

    private fun formatWon(value: Long): String = "₩${PRICE_FORMAT.format(value)}"

    private companion object {
        val PRICE_FORMAT: NumberFormat = NumberFormat.getNumberInstance(Locale.KOREA)
    }
}
