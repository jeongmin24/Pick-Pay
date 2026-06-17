package com.ssafy.payclient.ui.cart

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ssafy.payclient.data.local.PersonalCartItem
import com.ssafy.payclient.databinding.ItemCartBinding

class CartAdapter(
    private var cartItems: List<PersonalCartItem>,
    private val onIncreaseClick: (PersonalCartItem) -> Unit,
    private val onDecreaseClick: (PersonalCartItem) -> Unit,
    private val onRemoveClick: (PersonalCartItem) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    inner class CartViewHolder(private val binding: ItemCartBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PersonalCartItem) {
            binding.tvCartMenuName.text = item.menuName
            binding.tvCartMenuPrice.text = "${item.price} 원"
            binding.tvCartQuantity.text = item.quantity.toString()
            binding.tvCartItemTotal.text = "${item.price * item.quantity} 원"

            binding.btnIncrease.setOnClickListener {
                onIncreaseClick(item)
            }

            binding.btnDecrease.setOnClickListener {
                onDecreaseClick(item)
            }

            binding.btnRemove.setOnClickListener {
                onRemoveClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = ItemCartBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(cartItems[position])
    }

    override fun getItemCount(): Int = cartItems.size

    fun updateItems(newItems: List<PersonalCartItem>) {
        cartItems = newItems
        notifyDataSetChanged()
    }
}
