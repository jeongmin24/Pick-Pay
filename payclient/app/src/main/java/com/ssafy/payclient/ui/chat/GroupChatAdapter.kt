package com.ssafy.payclient.ui.chat

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ssafy.payclient.R
import com.ssafy.payclient.data.model.FirebaseChatMessage
import com.ssafy.payclient.databinding.ItemGroupChatMessageBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
            val context = binding.root.context

            params.gravity = if (isMine) Gravity.END else Gravity.START
            binding.layoutMessageContainer.layoutParams = params
            binding.tvSenderName.text = if (isMine) "" else displayName.ifBlank {
                "User ${message.senderId}"
            }
            binding.tvSenderName.visibility = if (isMine) View.GONE else View.VISIBLE
            binding.layoutMessageRow.layoutDirection =
                if (isMine) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
            binding.tvMessage.layoutDirection = View.LAYOUT_DIRECTION_LTR
            binding.tvMessage.text = message.message
            binding.tvMessage.setTextColor(
                ContextCompat.getColor(context, if (isMine) R.color.white else R.color.text_primary)
            )
            binding.tvMessage.setBackgroundResource(
                if (isMine) R.drawable.bg_chat_bubble_mine else R.drawable.bg_chat_bubble_other
            )
            binding.tvMessageTime.text = message.createdAt.toChatTime()
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

    private fun Long.toChatTime(): String {
        if (this <= 0L) return ""
        return TIME_FORMAT.format(Date(this))
    }

    companion object {
        private val TIME_FORMAT = SimpleDateFormat("a h:mm", Locale.KOREA)
    }
}
