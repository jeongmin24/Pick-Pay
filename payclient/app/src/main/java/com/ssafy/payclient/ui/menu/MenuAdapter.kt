package com.ssafy.payclient.ui.menu

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ssafy.payclient.data.model.MenuDTO
import com.ssafy.payclient.databinding.ItemMenuBinding


class MenuAdapter(
    private var menuList: List<MenuDTO>,
    private val onItemClick: (MenuDTO) -> Unit
) : RecyclerView.Adapter<MenuAdapter.MenuViewHolder>() {

    inner class MenuViewHolder(private val binding: ItemMenuBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(menu: MenuDTO) {
            binding.tvMenuName.text = menu.menuName
            binding.tvMenuPrice.text = "${menu.price}원"

            // 카드를 클릭했을 때 메뉴 담기
            binding.root.setOnClickListener {
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
        holder.bind(menuList[position])
    }

    override fun getItemCount(): Int = menuList.size

    fun updateList(newList: List<MenuDTO>) {
        menuList = newList
        notifyDataSetChanged()
    }
}