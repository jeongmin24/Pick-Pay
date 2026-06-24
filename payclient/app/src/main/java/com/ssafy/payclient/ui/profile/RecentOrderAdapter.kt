package com.ssafy.payclient.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ssafy.payclient.data.model.RecentOrderResponseDTO
import com.ssafy.payclient.databinding.ItemRecentOrderBinding
import java.text.NumberFormat
import java.util.Locale

class RecentOrderAdapter(
    private var orders: List<RecentOrderResponseDTO> = emptyList(),
    private val onOrderClick: (RecentOrderResponseDTO) -> Unit = {}
) : RecyclerView.Adapter<RecentOrderAdapter.RecentOrderViewHolder>() {

    inner class RecentOrderViewHolder(private val binding: ItemRecentOrderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(order: RecentOrderResponseDTO) {
            binding.tvRecentOrderName.text = buildOrderName(order)
            binding.tvRecentOrderStatus.text = order.status
            binding.tvRecentOrderNo.text = "Order ${order.orderNo}"
            binding.tvRecentOrderDate.text = formatDate(order.createdAt)
            binding.tvRecentOrderPrice.text = "${priceFormat.format(order.totalPrice)} won"
            binding.root.setOnClickListener {
                onOrderClick(order)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentOrderViewHolder {
        val binding = ItemRecentOrderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RecentOrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecentOrderViewHolder, position: Int) {
        holder.bind(orders[position])
    }

    override fun getItemCount(): Int = orders.size

    fun submitList(newOrders: List<RecentOrderResponseDTO>) {
        orders = newOrders
        notifyDataSetChanged()
    }

    private fun buildOrderName(order: RecentOrderResponseDTO): String {
        val firstMenuName = order.firstMenuName ?: "Order item"
        val extraCount = order.itemCount - 1
        return if (extraCount > 0) {
            "$firstMenuName + $extraCount"
        } else {
            firstMenuName
        }
    }

    private fun formatDate(createdAt: String): String {
        return createdAt
            .replace("T", " ")
            .substringBefore(".")
            .take(16)
    }

    companion object {
        private val priceFormat = NumberFormat.getNumberInstance(Locale.KOREA)
    }
}
