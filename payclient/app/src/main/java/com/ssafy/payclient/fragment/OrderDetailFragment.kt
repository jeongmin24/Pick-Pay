package com.ssafy.payclient.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ssafy.payclient.R
import com.ssafy.payclient.data.local.TokenManager
import com.ssafy.payclient.data.model.IndividualReceiptResponseDTO
import com.ssafy.payclient.data.network.RetrofitClient
import com.ssafy.payclient.databinding.FragmentOrderDetailBinding
import com.ssafy.payclient.ui.profile.OrderDetailItemAdapter
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch

class OrderDetailFragment : Fragment() {

    private var _binding: FragmentOrderDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var itemAdapter: OrderDetailItemAdapter

    private val orderNo: String by lazy {
        arguments?.getString(ARG_ORDER_NO).orEmpty()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupItems()

        if (orderNo.isBlank()) {
            showError("Order number is missing.")
        } else {
            loadOrderDetail(orderNo)
        }
    }

    private fun setupToolbar() {
        binding.toolbarOrderDetail.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupItems() {
        itemAdapter = OrderDetailItemAdapter()
        binding.rvOrderDetailItems.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = itemAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun loadOrderDetail(orderNo: String) {
        showLoading(true)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val tokenManager = TokenManager(requireContext().applicationContext)
                val apiService = RetrofitClient.getIndividualOrderApiService(tokenManager)
                val response = apiService.getReceipt(orderNo)

                if (response.isSuccessful) {
                    val receipt = response.body()
                    if (receipt == null) {
                        showError("Order detail is empty.")
                    } else {
                        renderOrderDetail(receipt)
                    }
                } else {
                    showError("Order detail failed: ${response.code()}")
                }
            } catch (e: Exception) {
                showError("Order detail error: ${e.message}")
            }
        }
    }

    private fun renderOrderDetail(receipt: IndividualReceiptResponseDTO) {
        val currentBinding = _binding ?: return
        currentBinding.progressOrderDetail.visibility = View.GONE
        currentBinding.groupOrderDetailContent.visibility = View.VISIBLE
        currentBinding.tvOrderDetailError.visibility = View.GONE

        currentBinding.tvOrderDetailNo.text = "Order ${receipt.displayOrderNo ?: orderNo}"
        currentBinding.tvOrderDetailStatus.text = receipt.status
        currentBinding.tvOrderDetailDate.text = formatDate(receipt.createdAt)
        currentBinding.tvOrderDetailTotal.text = "${formatPrice(receipt.totalPrice)} won"
        currentBinding.tvOrderDetailItemCount.text = "${receipt.items.size} items"

        itemAdapter.submitList(receipt.items)
    }

    private fun showLoading(isLoading: Boolean) {
        val currentBinding = _binding ?: return
        currentBinding.progressOrderDetail.visibility = if (isLoading) View.VISIBLE else View.GONE
        currentBinding.groupOrderDetailContent.visibility = if (isLoading) View.GONE else View.VISIBLE
        currentBinding.tvOrderDetailError.visibility = View.GONE
    }

    private fun showError(message: String) {
        val currentBinding = _binding ?: return
        currentBinding.progressOrderDetail.visibility = View.GONE
        currentBinding.groupOrderDetailContent.visibility = View.GONE
        currentBinding.tvOrderDetailError.visibility = View.VISIBLE
        currentBinding.tvOrderDetailError.text = message
        context?.let {
            Toast.makeText(it, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatDate(createdAt: String): String {
        return createdAt
            .replace("T", " ")
            .substringBefore(".")
            .take(16)
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

    companion object {
        const val ARG_ORDER_NO = "ORDER_NO"
        private val priceFormat = NumberFormat.getNumberInstance(Locale.KOREA)

        private fun formatPrice(value: Long): String = priceFormat.format(value)
    }
}
