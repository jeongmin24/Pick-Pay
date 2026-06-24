package com.ssafy.payclient.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ssafy.payclient.data.model.RecentOrderResponseDTO
import com.ssafy.payclient.databinding.ItemRecentOrderBinding
import java.text.NumberFormat
import java.util.Locale

class RecentOrderAdapter(
    private var orders: List<RecentOrderResponseDTO> = emptyList()
) : RecyclerView.Adapter<RecentOrderAdapter.RecentOrderViewHolder>() {

    inner class RecentOrderViewHolder(private val binding: ItemRecentOrderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(order: RecentOrderResponseDTO) {
            binding.tvRecentOrderName.text = buildOrderName(order)
            binding.tvRecentOrderStatus.text = formatStatus(order.status)
            binding.tvRecentOrderNo.text = "주문 ${order.orderNo}"
            binding.tvRecentOrderDate.text = formatDate(order.createdAt)
            binding.tvRecentOrderPrice.text = "${priceFormat.format(order.totalPrice)}원"
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
        val firstMenuName = order.firstMenuName ?: "주문 메뉴"
        val extraCount = order.itemCount - 1
        return if (extraCount > 0) {
            "$firstMenuName 외 ${extraCount}건"
        } else {
            firstMenuName
        }
    }

    private fun formatDate(createdAt: String): String {
        return createdAt
            .replace("T", " ")
            .substringBefore(".")
            .take(16)
            .replace("-", ".")
    }

    private fun formatStatus(status: String): String {
        return when (status.uppercase()) {
            "COMPLETED", "DONE", "PICKED_UP", "PAID" -> "수령 완료"
            "READY" -> "수령 대기"
            "CANCELED", "CANCELLED" -> "취소"
            else -> status
        }
    }

    companion object {
        private val priceFormat = NumberFormat.getNumberInstance(Locale.KOREA)
    }
}
