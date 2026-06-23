package com.ssafy.payclient.ui.chat

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import com.ssafy.payclient.data.model.FirebaseChatMessage
import com.ssafy.payclient.databinding.ItemGroupChatMessageBinding

class GroupChatAdapter(
    private val currentUserId: Long
) : RecyclerView.Adapter<GroupChatAdapter.GroupChatViewHolder>() {

    private var messages: List<FirebaseChatMessage> = emptyList()

    inner class GroupChatViewHolder(
        private val binding: ItemGroupChatMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: FirebaseChatMessage) {
            val isMine = message.senderId == currentUserId
            val params = binding.layoutMessageContainer.layoutParams as FrameLayout.LayoutParams
            val displayName = message.sendName.ifBlank { message.senderName }

            params.gravity = if (isMine) Gravity.END else Gravity.START
            binding.layoutMessageContainer.layoutParams = params
            binding.tvSenderName.text = if (isMine) "Me" else displayName.ifBlank {
                "User ${message.senderId}"
            }
            binding.tvMessage.text = message.message
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupChatViewHolder {
        val binding = ItemGroupChatMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GroupChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroupChatViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount(): Int = messages.size

    fun submitList(newMessages: List<FirebaseChatMessage>) {
        messages = newMessages
        notifyDataSetChanged()
    }
}
