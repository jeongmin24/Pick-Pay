package com.ssafy.payclient.fragment

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.ssafy.payclient.R
import com.ssafy.payclient.data.local.TokenManager
import com.ssafy.payclient.data.model.FirebaseChatMessage
import com.ssafy.payclient.data.network.RetrofitClient
import com.ssafy.payclient.databinding.FragmentGroupChatBinding
import com.ssafy.payclient.ui.chat.GroupChatAdapter
import kotlinx.coroutines.launch

class GroupChatFragment : Fragment() {

    private var _binding: FragmentGroupChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: DatabaseReference
    private lateinit var chatAdapter: GroupChatAdapter

    private var groupId: String = ""
    private var currentUserId: Long = -1L
    private var isHost: Boolean = false
    private var currentUserNickname: String = ""
    private var messagesQuery: Query? = null
    private var messagesListener: ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        groupId = arguments?.getString("GROUP_ID").orEmpty()
        currentUserId = arguments?.getLong("USER_ID") ?: -1L
        isHost = arguments?.getBoolean("IS_HOST") ?: false
        database = FirebaseDatabase.getInstance("https://pickpay-be337-default-rtdb.firebaseio.com/").reference

        setupToolbar()
        setupRecyclerView()
        setupMessageInput()
        loadCurrentUserNickname()
        observeMessages()
    }

    private fun setupToolbar() {
        binding.toolbarGroupChat.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupRecyclerView() {
        chatAdapter = GroupChatAdapter(currentUserId)

        binding.rvGroupChatMessages.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }
    }

    private fun setupMessageInput() {
        binding.btnSendGroupChat.setOnClickListener {
            sendCurrentMessage()
        }

        binding.etGroupChatMessage.setOnEditorActionListener { _, actionId, event ->
            val isSendAction = actionId == EditorInfo.IME_ACTION_SEND
            val isEnterUp = event?.let {
                it.keyCode == KeyEvent.KEYCODE_ENTER && it.action == KeyEvent.ACTION_UP
            } ?: false

            if (isSendAction || isEnterUp) {
                sendCurrentMessage()
                true
            } else {
                false
            }
        }
    }

    private fun observeMessages() {
        val query = getMessagesRef()
            .orderByChild("createdAt")
            .limitToLast(100)

        messagesQuery = query
        messagesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull {
                    it.getValue(FirebaseChatMessage::class.java)
                }

                renderMessages(messages)
            }

            override fun onCancelled(error: DatabaseError) {
                if (_binding == null) return
                Toast.makeText(
                    requireContext(),
                    "채팅을 불러오지 못했습니다: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        query.addValueEventListener(messagesListener!!)
    }

    private fun renderMessages(messages: List<FirebaseChatMessage>) {
        chatAdapter.submitList(messages)

        val isEmpty = messages.isEmpty()
        binding.tvEmptyGroupChat.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvGroupChatMessages.visibility = if (isEmpty) View.GONE else View.VISIBLE

        if (messages.isNotEmpty()) {
            binding.rvGroupChatMessages.post {
                binding.rvGroupChatMessages.scrollToPosition(messages.lastIndex)
            }
        }
    }

    private fun sendCurrentMessage() {
        val text = binding.etGroupChatMessage.text?.toString()?.trim().orEmpty()
        if (text.isBlank()) return

        if (groupId.isBlank()) {
            Toast.makeText(requireContext(), "단체 주문 정보를 확인할 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val message = mapOf(
                "senderId" to currentUserId,
                "sendName" to getOrLoadSendName(),
                "message" to text,
                "createdAt" to ServerValue.TIMESTAMP
            )

            getMessagesRef().push().setValue(message)
                .addOnSuccessListener {
                    if (_binding == null) return@addOnSuccessListener
                    binding.etGroupChatMessage.text?.clear()
                }
                .addOnFailureListener { error ->
                    if (_binding == null) return@addOnFailureListener
                    Toast.makeText(
                        requireContext(),
                        "메시지 전송 실패: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    private fun loadCurrentUserNickname() {
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                val tokenManager = TokenManager(requireContext().applicationContext)
                RetrofitClient.getUserApiService(tokenManager).getUserInfo()
            }.onSuccess { userResponse ->
                currentUserNickname = userResponse.nickname
            }
        }
    }

    private suspend fun getOrLoadSendName(): String {
        if (currentUserNickname.isBlank()) {
            runCatching {
                val tokenManager = TokenManager(requireContext().applicationContext)
                RetrofitClient.getUserApiService(tokenManager).getUserInfo()
            }.onSuccess { userResponse ->
                currentUserNickname = userResponse.nickname
            }
        }

        return currentUserNickname.ifBlank { "User $currentUserId" }
    }

    private fun getMessagesRef(): DatabaseReference {
        return database
            .child("group_orders")
            .child(groupId)
            .child("chat")
            .child("messages")
    }

    override fun onResume() {
        super.onResume()
        activity?.findViewById<View>(R.id.bottom_navigation)?.visibility = View.GONE
    }

    override fun onStop() {
        super.onStop()
        activity?.findViewById<View>(R.id.bottom_navigation)?.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        messagesListener?.let { listener ->
            messagesQuery?.removeEventListener(listener)
        }
        messagesListener = null
        messagesQuery = null
        _binding = null
    }
}
