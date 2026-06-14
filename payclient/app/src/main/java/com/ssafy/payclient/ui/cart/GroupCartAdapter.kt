package com.ssafy.payclient.ui.cart

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ssafy.payclient.data.model.FirebaseCartItem
import com.ssafy.payclient.databinding.ItemGroupCartBinding
import com.ssafy.payclient.databinding.ItemGroupCartMenuBinding

data class GroupCartItemUi(
    val itemKey: String,
    val item: FirebaseCartItem
)

data class GroupCartUserUi(
    val userId: Long,
    val items: List<GroupCartItemUi>
)

class GroupCartAdapter(
    cartItems: List<GroupCartItemUi>,
    private val currentUserId: Long,
    private val onIncreaseClick: (GroupCartItemUi) -> Unit,
    private val onDecreaseClick: (GroupCartItemUi) -> Unit,
    private val onRemoveClick: (GroupCartItemUi) -> Unit
) : RecyclerView.Adapter<GroupCartAdapter.GroupCartViewHolder>() {

    private var userGroups: List<GroupCartUserUi> = cartItems.toUserGroups()

    inner class GroupCartViewHolder(private val binding: ItemGroupCartBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(userGroup: GroupCartUserUi) {
            val isMine = userGroup.userId == currentUserId
            binding.tvGroupCartUser.text = if (isMine) {
                "User ${userGroup.userId} (Me)"
            } else {
                "User ${userGroup.userId}"
            }

            binding.layoutGroupCartMenus.removeAllViews()
            userGroup.items.forEach { cartItem ->
                val menuBinding = ItemGroupCartMenuBinding.inflate(
                    LayoutInflater.from(binding.root.context),
                    binding.layoutGroupCartMenus,
                    false
                )
                bindMenu(menuBinding, cartItem, isMine)
                binding.layoutGroupCartMenus.addView(menuBinding.root)
            }
        }

        private fun bindMenu(
            menuBinding: ItemGroupCartMenuBinding,
            cartItem: GroupCartItemUi,
            isMine: Boolean
        ) {
            val item = cartItem.item
            menuBinding.tvGroupCartMenuName.text = item.menuName
            menuBinding.tvGroupCartQuantity.text = "x${item.quantity}"
            menuBinding.layoutQuantityControls.visibility = if (isMine) View.VISIBLE else View.GONE

            menuBinding.btnGroupCartIncrease.setOnClickListener {
                onIncreaseClick(cartItem)
            }

            menuBinding.btnGroupCartDecrease.setOnClickListener {
                onDecreaseClick(cartItem)
            }

            menuBinding.btnGroupCartRemove.setOnClickListener {
                onRemoveClick(cartItem)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupCartViewHolder {
        val binding = ItemGroupCartBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GroupCartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroupCartViewHolder, position: Int) {
        holder.bind(userGroups[position])
    }

    override fun getItemCount(): Int = userGroups.size

    fun updateItems(newItems: List<GroupCartItemUi>) {
        userGroups = newItems.toUserGroups()
        notifyDataSetChanged()
    }

    private fun List<GroupCartItemUi>.toUserGroups(): List<GroupCartUserUi> {
        return groupBy { it.item.userId }
            .toSortedMap()
            .map { (userId, items) ->
                GroupCartUserUi(
                    userId = userId,
                    items = items.sortedBy { it.item.menuName }
                )
            }
    }
}
