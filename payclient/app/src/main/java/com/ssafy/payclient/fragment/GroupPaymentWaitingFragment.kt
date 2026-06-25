package com.ssafy.payclient.fragment

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.ssafy.payclient.R
import com.ssafy.payclient.data.local.TokenManager
import com.ssafy.payclient.data.model.OrderItemDTO
import com.ssafy.payclient.data.model.ReceiptResponseDTO
import com.ssafy.payclient.data.model.UserReceiptDTO
import com.ssafy.payclient.data.network.RetrofitClient
import com.ssafy.payclient.databinding.FragmentGroupPaymentWaitingBinding
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GroupPaymentWaitingFragment : Fragment() {

    private var _binding: FragmentGroupPaymentWaitingBinding? = null
    private val binding get() = _binding!!

    private val groupId: String by lazy { requireArguments().getString(ARG_GROUP_ID).orEmpty() }
    private val currentUserId: Long by lazy { requireArguments().getLong(ARG_USER_ID, -1L) }
    private var hasNavigatedAway = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupPaymentWaitingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupSystemBarInsets()
        setupButtons()
        observePaymentStatus()
    }

    private fun setupToolbar() {
        binding.toolbarGroupPaymentWaiting.setNavigationOnClickListener {
            navigateToOrderStart()
        }
    }

    private fun setupSystemBarInsets() {
        val bottomLayout = binding.layoutGroupPaymentBottom
        val baseBottomPadding = bottomLayout.paddingBottom
        val baseHeight = bottomLayout.layoutParams.height

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            bottomLayout.updatePadding(bottom = baseBottomPadding + bottomInset)
            bottomLayout.layoutParams = bottomLayout.layoutParams.apply {
                height = baseHeight + bottomInset
            }
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    private fun setupButtons() {
        binding.btnWaitingPay.setOnClickListener {
            val receipt = it.tag as? UserReceiptDTO ?: return@setOnClickListener
            navigateToPayment(receipt)
        }
        binding.btnWaitingOrder.setOnClickListener {
            navigateToOrderStart()
        }
        binding.btnWaitingHome.setOnClickListener {
            navigateHome()
        }
    }

    private fun observePaymentStatus() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive && !hasNavigatedAway) {
                    loadPaymentStatus()
                    delay(POLL_INTERVAL_MILLIS)
                }
            }
        }
    }

    private suspend fun loadPaymentStatus() {
        if (groupId.isBlank()) {
            showWaitingMessage("주문방 정보를 확인할 수 없어요.")
            return
        }

        try {
            val tokenManager = TokenManager(requireContext().applicationContext)
            val apiService = RetrofitClient.getGroupOrderApiService(tokenManager)
            val response = apiService.getGroupReceipt(groupId)

            if (!response.isSuccessful) {
                showWaitingMessage("결제 상태를 불러오는 중이에요. (${response.code()})")
                return
            }

            val receipt = response.body()
            if (receipt == null) {
                showWaitingMessage("결제 정보를 준비 중이에요.")
                return
            }

            renderStatus(receipt)
        } catch (e: Exception) {
            if (_binding == null) return
            showWaitingMessage("결제 상태 확인 중 오류가 발생했어요.")
        }
    }

    private fun renderStatus(receipt: ReceiptResponseDTO) {
        val myReceipt = receipt.userReceipts.firstOrNull { it.userId == currentUserId }
        val groupStatus = receipt.groupStatus.orEmpty()

        if (groupStatus == STATUS_PAYMENT_FAILED) {
            navigatePaymentFailure("단체 결제에 실패했어요.")
            return
        }

        binding.tvWaitingTotal.text = formatWon(receipt.totalGroupPrice)
        renderMemberStatusRows(receipt.userReceipts)

        when (groupStatus) {
            STATUS_PAID -> renderCompleted()
            else -> renderWaiting(myReceipt)
        }
    }

    private fun renderWaiting(myReceipt: UserReceiptDTO?) {
        binding.tvGroupPaymentToolbarTitle.text = "결제 대기"
        binding.groupPaymentCompleteIcon.visibility = View.GONE
        binding.progressWaiting.visibility = View.VISIBLE
        binding.tvWaitingGroupId.text = "총 주문 금액"
        binding.tvWaitingTitle.text = "결제를 기다리고 있어요"
        binding.tvWaitingDescription.text = when (myReceipt?.orderStatus) {
            STATUS_PAYMENT_APPROVED -> "내 결제는 승인됐고, 다른 멤버의 결제를 기다리는 중이에요."
            STATUS_PENDING -> "내 결제를 진행하면 완료 상태를 확인할 수 있어요."
            else -> "주문이 마감됐어요. 멤버들의 결제 상태를 확인하고 있어요."
        }
        binding.btnWaitingPay.visibility = if (myReceipt?.orderStatus == STATUS_PENDING) View.VISIBLE else View.GONE
        binding.btnWaitingPay.tag = myReceipt
        binding.btnWaitingOrder.visibility = View.VISIBLE
        binding.btnWaitingHome.visibility = View.GONE
    }

    private fun renderCompleted() {
        binding.tvGroupPaymentToolbarTitle.text = "결제 완료"
        binding.groupPaymentCompleteIcon.visibility = View.GONE
        binding.progressWaiting.visibility = View.GONE
        binding.tvWaitingGroupId.text = "최종 결제 금액"
        binding.tvWaitingTitle.text = "모든 결제가 완료됐어요"
        binding.tvWaitingDescription.text = "새로운 같이 주문은 방을 다시 만들거나 초대 링크로 새롭게 입장해주세요."
        binding.btnWaitingPay.visibility = View.GONE
        binding.btnWaitingOrder.visibility = View.VISIBLE
        binding.btnWaitingHome.visibility = View.VISIBLE
    }

    private fun showWaitingMessage(message: String) {
        binding.groupPaymentCompleteIcon.visibility = View.GONE
        binding.progressWaiting.visibility = View.VISIBLE
        binding.tvWaitingDescription.text = message
        binding.btnWaitingPay.visibility = View.GONE
    }

    private fun navigateToPayment(receipt: UserReceiptDTO) {
        val orderNo = receipt.orderNo
        if (orderNo.isNullOrBlank() || receipt.userTotalPrice <= 0L) {
            Toast.makeText(
                requireContext(),
                "결제 주문 정보를 확인할 수 없어요.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        findNavController().navigate(
            R.id.action_fragment_group_payment_waiting_to_fragment_payment,
            Bundle().apply {
                putString(PaymentFragment.ARG_ORDER_ID, orderNo)
                putLong(PaymentFragment.ARG_TOTAL_PRICE, receipt.userTotalPrice)
                putString(PaymentFragment.ARG_ORDER_NAME, buildGroupOrderName(receipt.items))
                putString(PaymentFragment.ARG_GROUP_ID, groupId)
                putLong(PaymentFragment.ARG_USER_ID, currentUserId)
            }
        )
    }

    private fun navigatePaymentFailure(message: String) {
        if (hasNavigatedAway) return
        hasNavigatedAway = true
        findNavController().navigate(
            R.id.fragment_payment_failure,
            Bundle().apply {
                putString(PaymentFailureFragment.ARG_TITLE, "결제 실패")
                putString(PaymentFailureFragment.ARG_MESSAGE, message)
            }
        )
    }

    private fun navigateToOrderStart() {
        val popped = findNavController().popBackStack(R.id.fragment_order, false)
        if (!popped) {
            findNavController().navigate(R.id.fragment_order)
        }
    }

    private fun navigateHome() {
        val popped = findNavController().popBackStack(R.id.fragment_home, false)
        if (!popped) {
            findNavController().navigate(R.id.fragment_home)
        }
    }

    private fun renderMemberStatusRows(userReceipts: List<UserReceiptDTO>) {
        binding.tvWaitingMembers.removeAllViews()

        if (userReceipts.isEmpty()) {
            binding.tvWaitingMembers.addView(TextView(requireContext()).apply {
                text = "결제 대상 멤버를 준비 중이에요."
                setTextColor(resources.getColor(R.color.text_secondary, null))
                textSize = 14f
            })
            return
        }

        userReceipts.forEachIndexed { index, receipt ->
            if (index > 0) {
                binding.tvWaitingMembers.addView(View(requireContext()).apply {
                    setBackgroundColor(resources.getColor(R.color.payment_divider, null))
                }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1.dp).apply {
                    topMargin = 18.dp
                    bottomMargin = 18.dp
                })
            }

            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val avatar = TextView(requireContext()).apply {
                text = receipt.nickname.orEmpty().ifBlank { "참" }.take(1)
                gravity = Gravity.CENTER
                setTextColor(resources.getColor(R.color.coffee_brown_dark, null))
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                setBackgroundResource(R.drawable.bg_group_payment_avatar)
            }
            row.addView(avatar, LinearLayout.LayoutParams(34.dp, 34.dp))

            val textColumn = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
            }
            row.addView(textColumn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = 14.dp
            })

            textColumn.addView(TextView(requireContext()).apply {
                text = receipt.nickname.orEmpty().ifBlank { "참여자" }
                setTextColor(resources.getColor(R.color.text_primary, null))
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                maxLines = 1
            })

            textColumn.addView(TextView(requireContext()).apply {
                text = formatWon(receipt.userTotalPrice)
                setTextColor(resources.getColor(R.color.coffee_brown_dark, null))
                textSize = 13f
            })

            row.addView(TextView(requireContext()).apply {
                text = "✓ ${receipt.orderStatus.groupPaymentStatusText()}"
                gravity = Gravity.CENTER
                setTextColor(resources.getColor(R.color.coffee_brown_dark, null))
                textSize = 13f
                setBackgroundResource(R.drawable.bg_group_payment_status_pill)
                setPadding(14.dp, 0, 14.dp, 0)
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 32.dp))

            binding.tvWaitingMembers.addView(row)
        }
    }

    private fun buildGroupOrderName(items: List<OrderItemDTO>): String {
        val firstItemName = items.firstOrNull()?.menuName ?: "단체 주문"
        return if (items.size <= 1) firstItemName else "$firstItemName 외 ${items.size - 1}건"
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
        _binding = null
    }

    companion object {
        const val ARG_GROUP_ID = "GROUP_ID"
        const val ARG_USER_ID = "USER_ID"
        private const val POLL_INTERVAL_MILLIS = 3_000L
        private const val STATUS_PENDING = "PENDING"
        private const val STATUS_PAYMENT_APPROVED = "PAYMENT_APPROVED"
        private const val STATUS_PAID = "PAID"
        private const val STATUS_PAYMENT_FAILED = "PAYMENT_FAILED"
        private const val STATUS_CANCELLED = "CANCELLED"
    }
}

private fun formatWon(value: Long): String {
    return "${NumberFormat.getNumberInstance(Locale.KOREA).format(value)}원"
}

private fun String?.groupPaymentStatusText(): String {
    return when (this) {
        "PAID", "PAYMENT_APPROVED" -> "결제 완료"
        "PAYMENT_FAILED" -> "결제 실패"
        "CANCELLED" -> "취소됨"
        else -> "결제 대기"
    }
}

private val Int.dp: Int
    get() = (this * android.content.res.Resources.getSystem().displayMetrics.density).toInt()
