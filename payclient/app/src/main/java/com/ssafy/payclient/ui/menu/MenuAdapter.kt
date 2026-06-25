package com.ssafy.payclient.ui.menu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ssafy.payclient.R
import com.ssafy.payclient.data.model.MenuDTO
import com.ssafy.payclient.databinding.ItemMenuBinding
import java.text.NumberFormat
import java.util.Locale

class MenuAdapter(
    private var menuList: List<MenuDTO>,
    private val onItemClick: (MenuDTO) -> Unit
) : RecyclerView.Adapter<MenuAdapter.MenuViewHolder>() {

    inner class MenuViewHolder(private val binding: ItemMenuBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(menu: MenuDTO, position: Int) {
            binding.tvMenuName.text = menu.name
            binding.tvMenuPrice.text = "₩${PRICE_FORMAT.format(menu.price)}"

            val badgeText = when (position % 4) {
                0 -> "Best"
                1 -> "New"
                2 -> ""
                else -> "Seasonal"
            }
            binding.tvMenuBadge.text = badgeText
            binding.tvMenuBadge.visibility = if (badgeText.isBlank()) View.INVISIBLE else View.VISIBLE

            Glide.with(binding.root.context)
                .load(menu.imageUrl)
                .centerCrop()
                .into(binding.ivMenuImage)

            binding.root.setOnClickListener {
                onItemClick(menu)
            }
            binding.btnAddMenu.setOnClickListener {
                onItemClick(menu)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MenuViewHolder {
        val binding = ItemMenuBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MenuViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: MenuViewHolder,
        position: Int
    ) {
        holder.bind(menuList[position], position)
    }

    override fun getItemCount(): Int = menuList.size

    fun updateList(newList: List<MenuDTO>) {
        menuList = newList
        notifyDataSetChanged()
    }

    private companion object {
        val PRICE_FORMAT: NumberFormat = NumberFormat.getNumberInstance(Locale.KOREA)
    }
}
