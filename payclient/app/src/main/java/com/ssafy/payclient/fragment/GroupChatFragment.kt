package com.ssafy.payclient.fragment

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Query
import com.google.firebase.database.ServerValue
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.ssafy.payclient.R
import com.ssafy.payclient.data.local.TokenManager
import com.ssafy.payclient.data.model.FirebaseChatMessage
import com.ssafy.payclient.data.model.PickupCandidate
import com.ssafy.payclient.data.model.PickupRouletteResponse
import com.ssafy.payclient.data.network.RetrofitClient
import com.ssafy.payclient.databinding.FragmentGroupChatBinding
import com.ssafy.payclient.ui.chat.GroupChatAdapter
import kotlinx.coroutines.launch
import kotlin.math.max

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
    private var pickupRouletteListener: ValueEventListener? = null
    private var currentRouletteAnimator: ValueAnimator? = null
    private var animatingRouletteRoundId: String? = null
    private var completedRouletteRoundId: String? = null
    private var isRouletteRunning: Boolean = false
    private var isRouletteExpanded: Boolean = false

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
        setupWindowInsets()
        setupMessageInput()
        setupPickupRoulette()
        loadCurrentUserNickname()
        observeMessages()
        observePickupRoulette()
    }

    private fun setupToolbar() {
        binding.toolbarGroupChat.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val bottomInset = max(systemBottom, imeBottom)

            binding.layoutGroupChatInput.updateLayoutParams<ConstraintLayout.LayoutParams> {
                bottomMargin = bottomInset
            }

            if (imeBottom > 0 && ::chatAdapter.isInitialized && chatAdapter.itemCount > 0) {
                binding.rvGroupChatMessages.post {
                    binding.rvGroupChatMessages.scrollToPosition(chatAdapter.itemCount - 1)
                }
            }

            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
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

    private fun setupPickupRoulette() {
        binding.viewPickupRoulette.setCandidates(emptyList())
        binding.btnRunPickupRoulette.visibility = if (isHost) View.VISIBLE else View.GONE
        binding.btnRunPickupRoulette.setOnClickListener {
            runPickupRoulette()
        }
        binding.btnTogglePickupRoulette.setOnClickListener {
            isRouletteExpanded = !isRouletteExpanded
            renderRouletteExpansion()
        }
        renderRouletteExpansion()

        if (!isHost) {
            binding.tvPickupRouletteStatus.text = "방장이 픽업 담당자를 뽑으면 결과가 표시됩니다."
        }
    }

    private fun renderRouletteExpansion() {
        binding.layoutPickupRouletteContent.visibility =
            if (isRouletteExpanded) View.VISIBLE else View.GONE
        binding.btnTogglePickupRoulette.rotation = if (isRouletteExpanded) 0f else 180f
        binding.btnTogglePickupRoulette.contentDescription =
            if (isRouletteExpanded) "픽업 룰렛 접기" else "픽업 룰렛 펼치기"
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

    private fun observePickupRoulette() {
        pickupRouletteListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (_binding == null || !snapshot.exists()) return

                val response = snapshot.toPickupRouletteResponse() ?: return
                renderPickupRoulette(response)

                if (response.status == ROULETTE_STATUS_SPINNING) {
                    animatePickupWinner(response)
                } else {
                    if (animatingRouletteRoundId == response.stableRoundId()) return

                    currentRouletteAnimator?.removeAllListeners()
                    currentRouletteAnimator?.cancel()
                    currentRouletteAnimator = null
                    animatingRouletteRoundId = null
                    isRouletteRunning = false
                    renderPickupWinnerResult(response, "픽업 담당자가 선정되었습니다.")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (_binding == null) return
                Toast.makeText(
                    requireContext(),
                    "룰렛 결과를 불러오지 못했습니다: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        getPickupRouletteRef().addValueEventListener(pickupRouletteListener!!)
    }

    private fun renderMessages(messages: List<FirebaseChatMessage>) {
        chatAdapter.submitList(messages)

        val isEmpty = messages.isEmpty()
        binding.layoutEmptyGroupChat.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.tvEmptyGroupChat.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvGroupChatMessages.visibility = if (isEmpty) View.GONE else View.VISIBLE

        if (messages.isNotEmpty()) {
            binding.rvGroupChatMessages.post {
                binding.rvGroupChatMessages.scrollToPosition(messages.lastIndex)
            }
        }
    }

    private fun runPickupRoulette() {
        if (groupId.isBlank()) {
            Toast.makeText(requireContext(), "단체 주문 정보를 확인할 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        if (isRouletteRunning) return

        isRouletteRunning = true
        isRouletteExpanded = true
        renderRouletteExpansion()
        binding.btnRunPickupRoulette.isEnabled = false
        binding.btnRunPickupRoulette.text = "진행 중"
        binding.tvPickupRouletteStatus.text = "후보를 불러와 룰렛을 돌리고 있습니다."

        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                val tokenManager = TokenManager(requireContext().applicationContext)
                RetrofitClient.getGroupOrderApiService(tokenManager).selectPickupWinner(groupId)
            }.onSuccess { apiResponse ->
                if (_binding == null) return@onSuccess

                val body = apiResponse.body()
                if (apiResponse.isSuccessful && body != null) {
                    if (body.alreadySelected) {
                        isRouletteRunning = false
                        renderPickupRoulette(body)
                        renderPickupWinnerResult(body, "이미 선정된 픽업 담당자입니다.")
                    } else {
                        renderPickupRoulette(body)
                        animatePickupWinner(body)
                    }
                } else {
                    isRouletteRunning = false
                    binding.btnRunPickupRoulette.isEnabled = true
                    binding.btnRunPickupRoulette.text = "시작"
                    val errorMessage = runCatching { apiResponse.errorBody()?.string() }.getOrNull()
                        ?.takeIf { it.isNotBlank() }
                        ?: "픽업 룰렛을 실행하지 못했습니다."
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                    binding.tvPickupRouletteStatus.text = "장바구니에 담긴 멤버가 있어야 실행할 수 있습니다."
                }
            }.onFailure { error ->
                if (_binding == null) return@onFailure
                isRouletteRunning = false
                binding.btnRunPickupRoulette.isEnabled = true
                binding.btnRunPickupRoulette.text = "시작"
                binding.tvPickupRouletteStatus.text = "룰렛 실행에 실패했습니다."
                Toast.makeText(
                    requireContext(),
                    "룰렛 실행 실패: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun renderPickupRoulette(response: PickupRouletteResponse) {
        val names = response.candidates.map { candidate ->
            candidate.nickname?.takeIf { it.isNotBlank() } ?: "User ${candidate.userId}"
        }
        binding.viewPickupRoulette.setCandidates(names)
    }

    private fun animatePickupWinner(response: PickupRouletteResponse) {
        if (response.candidates.isEmpty()) {
            isRouletteRunning = false
            binding.btnRunPickupRoulette.isEnabled = true
            binding.btnRunPickupRoulette.text = "시작"
            return
        }

        val roundId = response.stableRoundId()
        if (animatingRouletteRoundId == roundId || completedRouletteRoundId == roundId) return

        currentRouletteAnimator?.cancel()
        animatingRouletteRoundId = roundId
        isRouletteRunning = true
        isRouletteExpanded = true
        renderRouletteExpansion()
        binding.btnRunPickupRoulette.isEnabled = false
        binding.btnRunPickupRoulette.text = "진행 중"
        binding.tvPickupRouletteStatus.text = "픽업 담당자 룰렛이 돌아가고 있습니다."

        val durationMs = response.durationMs.takeIf { it > 0L } ?: PICKUP_ROULETTE_DURATION_MS
        val elapsedMs = if (response.startedAt > 0L) {
            System.currentTimeMillis() - response.startedAt
        } else {
            0L
        }
        val remainingMs = when {
            response.startedAt <= 0L -> durationMs
            elapsedMs < 0L -> durationMs
            elapsedMs >= durationMs -> durationMs
            else -> (durationMs - elapsedMs).coerceAtLeast(MIN_ROULETTE_ANIMATION_MS)
        }
        val rounds = if (remainingMs > 1200L) 7 else 2
        val targetRotation = binding.viewPickupRoulette.computeTargetRotation(response.winnerIndex, rounds)

        currentRouletteAnimator = ValueAnimator.ofFloat(
            binding.viewPickupRoulette.wheelRotation,
            targetRotation
        ).apply {
            duration = remainingMs
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                binding.viewPickupRoulette.wheelRotation = animator.animatedValue as Float
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (_binding == null) return
                    finishPickupRoulette(response)
                }
            })
            start()
        }
    }

    private fun finishPickupRoulette(response: PickupRouletteResponse) {
        if (_binding == null) return

        completedRouletteRoundId = response.stableRoundId()
        animatingRouletteRoundId = null
        currentRouletteAnimator = null
        isRouletteRunning = false
        renderPickupWinnerResult(response, "픽업 담당자가 선정되었습니다.")

        if (isHost) {
            pushPickupWinnerChatOnce(response)
        }
    }

    private fun renderPickupWinnerResult(response: PickupRouletteResponse, status: String) {
        val winnerName = response.winnerNickname?.takeIf { it.isNotBlank() }
            ?: "User ${response.winnerUserId}"

        binding.tvPickupRouletteStatus.text = status
        binding.tvPickupRouletteResult.text = "$winnerName 님이 픽업 담당자입니다."
        binding.btnRunPickupRoulette.isEnabled = isHost
        binding.btnRunPickupRoulette.text = if (isHost) "\uB2E4\uC2DC \uB3CC\uB9AC\uAE30" else "\uC120\uC815 \uC644\uB8CC"
    }

    private fun pushPickupWinnerChatOnce(response: PickupRouletteResponse) {
        getPickupRouletteRef().child("chatPushed").runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val alreadyPushed = currentData.getValue(Boolean::class.java) ?: false
                if (alreadyPushed) return Transaction.abort()

                currentData.value = true
                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                currentData: DataSnapshot?
            ) {
                if (!committed || error != null) return

                val winnerName = response.winnerNickname?.takeIf { it.isNotBlank() }
                    ?: "User ${response.winnerUserId}"
                val message = mapOf(
                    "senderId" to PICKPAY_SYSTEM_SENDER_ID,
                    "sendName" to PICKPAY_SYSTEM_SENDER_NAME,
                    "message" to "$winnerName 님이 픽업 담당자로 선정되었습니다.",
                    "createdAt" to ServerValue.TIMESTAMP
                )
                val rouletteUpdates = mapOf(
                    "status" to ROULETTE_STATUS_FINISHED,
                    "finishedAt" to ServerValue.TIMESTAMP
                )

                getMessagesRef().push().setValue(message)
                getPickupRouletteRef().updateChildren(rouletteUpdates)
            }
        })
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

    private fun DataSnapshot.toPickupRouletteResponse(): PickupRouletteResponse? {
        val winnerUserId = child("winnerUserId").getValue(Long::class.java) ?: return null
        val winnerNickname = child("winnerNickname").getValue(String::class.java)
        val candidates = child("candidates").children.mapNotNull { candidateSnapshot ->
            val userId = candidateSnapshot.child("userId").getValue(Long::class.java)
                ?: return@mapNotNull null
            val nickname = candidateSnapshot.child("nickname").getValue(String::class.java)
            PickupCandidate(userId, nickname)
        }
        val winnerIndex = child("winnerIndex").getValue(Long::class.java)?.toInt()
            ?: candidates.indexOfFirst { it.userId == winnerUserId }.takeIf { it >= 0 }
            ?: 0
        val startedAt = child("startedAt").getValue(Long::class.java)
            ?: child("selectedAt").getValue(Long::class.java)
            ?: 0L
        val status = child("status").getValue(String::class.java)
            ?: ROULETTE_STATUS_FINISHED

        return PickupRouletteResponse(
            groupId = groupId,
            roundId = child("roundId").getValue(String::class.java).orEmpty(),
            status = status,
            startedAt = startedAt,
            durationMs = child("durationMs").getValue(Long::class.java) ?: PICKUP_ROULETTE_DURATION_MS,
            winnerUserId = winnerUserId,
            winnerNickname = winnerNickname,
            winnerIndex = winnerIndex,
            alreadySelected = true,
            chatPushed = child("chatPushed").getValue(Boolean::class.java) ?: false,
            candidates = candidates
        )
    }

    private fun PickupRouletteResponse.stableRoundId(): String {
        return roundId.ifBlank { "$groupId-$startedAt-$winnerUserId-$winnerIndex" }
    }

    private fun getMessagesRef(): DatabaseReference {
        return database
            .child("group_orders")
            .child(groupId)
            .child("chat")
            .child("messages")
    }

    private fun getPickupRouletteRef(): DatabaseReference {
        return database
            .child("group_orders")
            .child(groupId)
            .child("pickupRoulette")
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
        pickupRouletteListener?.let { listener ->
            getPickupRouletteRef().removeEventListener(listener)
        }
        currentRouletteAnimator?.removeAllListeners()
        currentRouletteAnimator?.cancel()
        messagesListener = null
        messagesQuery = null
        pickupRouletteListener = null
        currentRouletteAnimator = null
        _binding = null
    }

    companion object {
        private const val PICKUP_ROULETTE_DURATION_MS = 3200L
        private const val MIN_ROULETTE_ANIMATION_MS = 1800L
        private const val ROULETTE_STATUS_SPINNING = "SPINNING"
        private const val ROULETTE_STATUS_FINISHED = "FINISHED"
        private const val PICKPAY_SYSTEM_SENDER_ID = 0L
        private const val PICKPAY_SYSTEM_SENDER_NAME = "PickPay"
    }
}
