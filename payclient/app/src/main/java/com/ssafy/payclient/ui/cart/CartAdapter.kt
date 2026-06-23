package com.ssafy.payclient.ui.cart

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ssafy.payclient.R
import com.ssafy.payclient.data.local.PersonalCartItem
import com.ssafy.payclient.databinding.ItemCartBinding
import java.text.NumberFormat
import java.util.Locale

class CartAdapter(
    private var cartItems: List<PersonalCartItem>,
    private val onIncreaseClick: (PersonalCartItem) -> Unit,
    private val onDecreaseClick: (PersonalCartItem) -> Unit,
    private val onRemoveClick: (PersonalCartItem) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    inner class CartViewHolder(private val binding: ItemCartBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PersonalCartItem, position: Int) {
            binding.tvCartMenuName.text = item.menuName
            binding.tvCartOption.text = optionText(position)
            binding.tvCartMenuPrice.text = formatWon(item.price)
            binding.tvCartQuantity.text = item.quantity.toString()
            binding.tvCartItemTotal.text = formatWon(item.price * item.quantity)
            binding.ivCartMenuImage.setImageResource(imageFor(position))

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
        holder.bind(cartItems[position], position)
    }

    override fun getItemCount(): Int = cartItems.size

    fun updateItems(newItems: List<PersonalCartItem>) {
        cartItems = newItems
        notifyDataSetChanged()
    }

    private fun optionText(position: Int): String {
        return when (position % 3) {
            0 -> "ICE / Regular"
            1 -> "HOT / Large"
            else -> "Warm up"
        }
    }

    private fun imageFor(position: Int): Int {
        return when (position % 3) {
            0 -> R.drawable.ic_latte_cup
            1 -> R.drawable.ic_matcha_parfait
            else -> R.drawable.ic_parfait_hero
        }
    }

    private fun formatWon(value: Long): String = "₩${PRICE_FORMAT.format(value)}"

    private companion object {
        val PRICE_FORMAT: NumberFormat = NumberFormat.getNumberInstance(Locale.KOREA)
    }
}
