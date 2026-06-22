package com.ssafy.payclient.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ssafy.payclient.R
import com.ssafy.payclient.data.local.PersonalCartStore
import com.ssafy.payclient.databinding.ActivityCartBinding
import com.ssafy.payclient.ui.cart.CartAdapter

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

        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        renderCart()
    }

    private fun setupToolbar() {
        binding.toolbarCart.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
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
            if (PersonalCartStore.getItems().isEmpty()) {
                Toast.makeText(requireContext(), "장바구니가 비었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Order API will be connected next.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun renderCart() {
        val items = PersonalCartStore.getItems()
        cartAdapter.updateItems(items)

        val isEmpty = items.isEmpty()
        binding.tvEmptyCart.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvCartItems.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.btnOrder.isEnabled = !isEmpty
        binding.tvTotalPrice.text = "${PersonalCartStore.getTotalPrice()} 원"
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
}
