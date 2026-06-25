package com.ssafy.payclient.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ssafy.payclient.data.model.OrderItemDTO
import com.ssafy.payclient.databinding.ItemOrderDetailMenuBinding
import java.text.NumberFormat
import java.util.Locale

class OrderDetailItemAdapter(
    private var items: List<OrderItemDTO> = emptyList()
) : RecyclerView.Adapter<OrderDetailItemAdapter.OrderDetailItemViewHolder>() {

    inner class OrderDetailItemViewHolder(private val binding: ItemOrderDetailMenuBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: OrderItemDTO) {
            val subtotal = item.price * item.quantity
            binding.tvOrderDetailItemName.text = item.menuName ?: "Menu item"
            binding.tvOrderDetailItemQuantity.text = "수량 ${item.quantity}"
            binding.tvOrderDetailItemUnitPrice.text = "${priceFormat.format(item.price)}원"
            binding.tvOrderDetailItemSubtotal.text = "${priceFormat.format(subtotal)}원"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderDetailItemViewHolder {
        val binding = ItemOrderDetailMenuBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OrderDetailItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderDetailItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun submitList(newItems: List<OrderItemDTO>) {
        items = newItems
        notifyDataSetChanged()
    }

    companion object {
        private val priceFormat = NumberFormat.getNumberInstance(Locale.KOREA)
    }
}
