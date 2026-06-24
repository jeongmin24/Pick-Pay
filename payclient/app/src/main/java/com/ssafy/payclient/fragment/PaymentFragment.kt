package com.ssafy.payclient.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.ssafy.payclient.BuildConfig
import com.ssafy.payclient.MainActivity
import com.ssafy.payclient.R
import com.ssafy.payclient.data.local.PersonalCartStore
import com.ssafy.payclient.data.local.TokenManager
import com.ssafy.payclient.data.model.IndividualReceiptResponseDTO
import com.ssafy.payclient.data.model.PaymentCompleteRequest
import com.ssafy.payclient.data.model.PaymentFailRequest
import com.ssafy.payclient.data.network.RetrofitClient
import com.ssafy.payclient.databinding.FragmentPaymentBinding
import com.tosspayments.paymentsdk.TossPayments
import com.tosspayments.paymentsdk.model.TossPaymentResult
import com.tosspayments.paymentsdk.model.paymentinfo.TossPaymentInfo
import com.tosspayments.paymentsdk.model.paymentinfo.TossPaymentMethod
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch

class PaymentFragment : Fragment() {

    private var _binding: FragmentPaymentBinding? = null
    private val binding get() = _binding!!

    private val tossPayments by lazy { TossPayments(BuildConfig.TOSS_CLIENT_KEY) }
    private val paymentResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handlePaymentResult(result.resultCode, result.data)
    }

    private val orderId: String by lazy { requireArguments().getString(ARG_ORDER_ID).orEmpty() }
    private val orderName: String by lazy { requireArguments().getString(ARG_ORDER_NAME).orEmpty() }
    private val totalPrice: Long by lazy { requireArguments().getLong(ARG_TOTAL_PRICE) }
    private val groupId: String by lazy { requireArguments().getString(ARG_GROUP_ID).orEmpty() }
    private val currentUserId: Long by lazy { requireArguments().getLong(ARG_USER_ID, -1L) }
    private var isPaymentCompleting = false
    private var isPaymentFailureHandling = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaymentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupSystemBarInsets()
        setupPaymentInfo()
        setupClickListeners()
        startPaymentPulse()
        observePaymentDeepLink()
    }

    private fun setupToolbar() {
        binding.toolbarPayment.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupSystemBarInsets() {
        val bottomBar = binding.paymentBottomBar
        val baseBottomPadding = bottomBar.paddingBottom
        val baseHeight = bottomBar.layoutParams.height

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            bottomBar.updatePadding(bottom = baseBottomPadding + bottomInset)
            bottomBar.layoutParams = bottomBar.layoutParams.apply {
                height = baseHeight + bottomInset
            }
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    private fun setupPaymentInfo() {
        binding.tvPaymentToolbarTitle.text = "Payment"
        binding.paymentPulseContainer.visibility = View.VISIBLE
        binding.tvPaymentAmountLabel.text = "TOTAL AMOUNT"
        binding.tvPaymentDescription.visibility = View.VISIBLE
        binding.layoutPaymentInfoCards.visibility = View.VISIBLE
        binding.receiptSuccessIcon.visibility = View.GONE
        binding.receiptCard.visibility = View.GONE
        binding.ivReceiptCups.visibility = View.GONE
        binding.tvPaymentAmount.text = "${formatPrice(totalPrice)} won"
        binding.tvPaymentDescription.text = "Press Pay to open the card payment window."
        binding.btnPayment.isEnabled = true
        binding.btnHome.visibility = View.GONE
        binding.tvReceiptDetails.visibility = View.GONE
    }

    private fun setupClickListeners() {
        binding.btnPayment.setOnClickListener {
            requestPayment()
        }
        binding.btnHome.setOnClickListener {
            navigateHome()
        }
    }

    private fun startPaymentPulse() {
        val backPulse = AnimationUtils.loadAnimation(requireContext(), R.anim.payment_pulse)
        val frontPulse = AnimationUtils.loadAnimation(requireContext(), R.anim.payment_pulse).apply {
            startOffset = 750L
        }
        binding.paymentPulseRingBack.startAnimation(backPulse)
        binding.paymentPulseRingFront.startAnimation(frontPulse)
    }

    private fun requestPayment() {
        val paymentInfo = TossPaymentInfo(
            orderId = orderId,
            orderName = orderName.ifBlank { "PickPay order" },
            amount = totalPrice
        )

        tossPayments.requestPayment(
            requireActivity(),
            TossPaymentMethod.Card,
            paymentInfo,
            paymentResultLauncher
        )
    }

    private fun handlePaymentResult(resultCode: Int, data: Intent?) {
        when (resultCode) {
            TossPayments.RESULT_PAYMENT_SUCCESS -> {
                val success = data?.getParcelableExtra(TossPayments.EXTRA_PAYMENT_RESULT_SUCCESS)
                    as? TossPaymentResult.Success

                if (success == null) {
                    Toast.makeText(requireContext(), "Payment result is missing.", Toast.LENGTH_SHORT).show()
                    return
                }

                completePayment(success)
            }

            TossPayments.RESULT_PAYMENT_FAILED -> {
                val fail = data?.getParcelableExtra(TossPayments.EXTRA_PAYMENT_RESULT_FAILED)
                    as? TossPaymentResult.Fail
                Toast.makeText(
                    requireContext(),
                    fail?.errorMessage ?: "Payment failed.",
                    Toast.LENGTH_SHORT
                ).show()
                handlePaymentFailure(fail?.errorMessage ?: "Payment failed.")
            }

            else -> {
                Toast.makeText(
                    requireContext(),
                    "Payment was canceled.",
                    Toast.LENGTH_SHORT
                ).show()
                handlePaymentFailure("Payment was canceled.")
            }
        }
    }

    private fun completePayment(success: TossPaymentResult.Success) {
        Log.d(
            TAG,
            "Toss success paymentKey=${success.paymentKey.maskPaymentKey()}, " +
                "orderId=${success.orderId}, amount=${success.amount}, localTotalPrice=$totalPrice"
        )
        completePayment(
            paymentKey = success.paymentKey,
            approvedOrderId = success.orderId,
            amount = totalPrice
        )
    }

    private fun completePayment(paymentKey: String, approvedOrderId: String, amount: Long) {
        if (isPaymentCompleting) return
        isPaymentCompleting = true
        binding.btnPayment.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val tokenManager = TokenManager(requireContext().applicationContext)
                val apiService = RetrofitClient.getPaymentApiService(tokenManager)
                val request = PaymentCompleteRequest(
                    paymentKey = paymentKey,
                    orderId = approvedOrderId,
                    amount = amount
                )

                val response = apiService.completePayment(request)

                if (response.isSuccessful) {
                    val body = response.body()
                    PersonalCartStore.clear()
                    Toast.makeText(
                        requireContext(),
                        body?.message ?: "Payment completed.",
                        Toast.LENGTH_SHORT
                    ).show()

                    if (groupId.isNotBlank()) {
                        navigatePaymentWaiting()
                    } else {
                        fetchReceipt(approvedOrderId)
                    }
                } else {
                    Log.e(
                        TAG,
                        "Payment complete failed status=${response.code()}, " +
                            "body=${response.errorBody()?.string()}"
                    )
                    Toast.makeText(
                        requireContext(),
                        "Payment confirmation failed. (${response.code()})",
                        Toast.LENGTH_SHORT
                    ).show()
                    isPaymentCompleting = false
                    _binding?.btnPayment?.isEnabled = true
                }
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Payment confirmation error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                isPaymentCompleting = false
                _binding?.btnPayment?.isEnabled = true
            }
        }
    }

    private fun handlePaymentFailure(reason: String) {
        if (isPaymentFailureHandling) return
        isPaymentFailureHandling = true
        binding.btnPayment.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            notifyPaymentFailed(reason)
            if (_binding != null) {
                navigatePaymentFailure(reason)
            }
        }
    }

    private suspend fun notifyPaymentFailed(reason: String) {
        try {
            if (orderId.isBlank()) return
            val tokenManager = TokenManager(requireContext().applicationContext)
            val apiService = RetrofitClient.getPaymentApiService(tokenManager)
            val response = apiService.failPayment(
                PaymentFailRequest(
                    orderId = orderId,
                    reason = reason
                )
            )

            if (!response.isSuccessful) {
                Log.e(
                    TAG,
                    "Payment fail notify failed status=${response.code()}, " +
                        "body=${response.errorBody()?.string()}"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Payment fail notify error", e)
        }
    }

    private suspend fun fetchReceipt(orderNo: String) {
        try {
            val tokenManager = TokenManager(requireContext().applicationContext)
            val apiService = RetrofitClient.getIndividualOrderApiService(tokenManager)
            val response = apiService.getReceipt(orderNo)

            if (response.isSuccessful) {
                val receipt = response.body()
                if (receipt == null) {
                    showReceiptLoadFailure("Receipt response is empty.")
                    return
                }
                renderReceipt(receipt, orderNo)
            } else {
                Log.e(
                    TAG,
                    "Receipt fetch failed orderNo=$orderNo, status=${response.code()}, " +
                        "body=${response.errorBody()?.string()}"
                )
                showReceiptLoadFailure("Receipt failed. (${response.code()})")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Receipt fetch error orderNo=$orderNo", e)
            showReceiptLoadFailure("Receipt error: ${e.message}")
        }
    }

    private fun renderReceipt(receipt: IndividualReceiptResponseDTO, orderNo: String) {
        binding.tvPaymentToolbarTitle.text = "Receipt"
        binding.paymentPulseContainer.visibility = View.GONE
        binding.paymentPulseRingBack.clearAnimation()
        binding.paymentPulseRingFront.clearAnimation()
        binding.receiptSuccessIcon.visibility = View.VISIBLE
        binding.tvPaymentAmountLabel.text = "PAYMENT COMPLETED"
        binding.tvPaymentAmount.text = "${formatPrice(receipt.totalPrice)} won"
        binding.tvPaymentDescription.visibility = View.GONE
        binding.layoutPaymentInfoCards.visibility = View.GONE
        binding.receiptCard.visibility = View.VISIBLE
        binding.ivReceiptCups.visibility = View.VISIBLE
        binding.tvReceiptOrderNo.text = "Order #${receipt.displayOrderNo ?: orderNo}"
        binding.tvReceiptTime.text = receipt.createdAt.toReceiptDateTime()
        binding.tvReceiptMethod.text = "Hygge Pay"
        binding.tvReceiptTotal.text = "${formatPrice(receipt.totalPrice)} won"
        renderReceiptItems(receipt)
        binding.tvReceiptDetails.visibility = View.GONE
        binding.btnPayment.visibility = View.GONE
        binding.btnHome.visibility = View.VISIBLE
        binding.toolbarPayment.setOnClickListener {
            navigateHome()
        }
        isPaymentCompleting = false
    }

    private fun renderReceiptItems(receipt: IndividualReceiptResponseDTO) {
        binding.layoutReceiptItems.removeAllViews()
        receipt.items.forEachIndexed { index, item ->
            val row = ConstraintLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    if (index > 0) topMargin = 16.dp
                }
            }

            val thumb = ImageView(requireContext()).apply {
                id = View.generateViewId()
                setBackgroundResource(R.drawable.bg_receipt_item_thumb)
                setImageResource(item.menuName.orEmpty().receiptIconRes())
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            row.addView(thumb, ConstraintLayout.LayoutParams(46.dp, 46.dp).apply {
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            })

            val name = TextView(requireContext()).apply {
                id = View.generateViewId()
                text = item.menuName.orEmpty().ifBlank { "Menu" }
                setTextColor(resources.getColor(R.color.text_primary, null))
                textSize = 18f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                maxLines = 1
            }
            row.addView(name, ConstraintLayout.LayoutParams(0, ConstraintLayout.LayoutParams.WRAP_CONTENT).apply {
                marginStart = 14.dp
                marginEnd = 12.dp
                startToEnd = thumb.id
                endToStart = View.generateViewId()
                topToTop = thumb.id
            })

            val price = TextView(requireContext()).apply {
                id = View.generateViewId()
                text = "${formatPrice(item.price * item.quantity)} won"
                setTextColor(resources.getColor(R.color.payment_primary, null))
                textSize = 18f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            row.addView(price, ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT).apply {
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                baselineToBaseline = name.id
            })

            val nameParams = name.layoutParams as ConstraintLayout.LayoutParams
            nameParams.endToStart = price.id
            name.layoutParams = nameParams

            val meta = TextView(requireContext()).apply {
                text = if (item.quantity > 1) "Qty ${item.quantity}" else "Size: Regular / Hot"
                setTextColor(resources.getColor(R.color.coffee_brown_dark, null))
                textSize = 14f
                maxLines = 1
            }
            row.addView(meta, ConstraintLayout.LayoutParams(0, ConstraintLayout.LayoutParams.WRAP_CONTENT).apply {
                marginStart = 14.dp
                marginEnd = 12.dp
                startToEnd = thumb.id
                endToStart = price.id
                topToBottom = name.id
            })

            binding.layoutReceiptItems.addView(row)
        }
    }

    private fun showReceiptLoadFailure(message: String) {
        binding.tvPaymentDescription.text = message
        binding.btnPayment.visibility = View.GONE
        binding.btnHome.visibility = View.VISIBLE
        binding.toolbarPayment.setOnClickListener {
            navigateHome()
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        isPaymentCompleting = false
    }

    private fun buildReceiptText(receipt: IndividualReceiptResponseDTO, orderNo: String): String {
        val itemLines = receipt.items.joinToString(separator = "\n") { item ->
            val subtotal = item.price * item.quantity
            "${item.menuName.orEmpty()} x ${item.quantity}  ${formatPrice(subtotal)} won"
        }

        return buildString {
            appendLine("Order No. ${receipt.displayOrderNo ?: orderNo}")
            appendLine("Status: ${receipt.status}")
            appendLine("Ordered at: ${receipt.createdAt}")
            appendLine()
            appendLine("Items")
            appendLine(itemLines.ifBlank { "No items" })
            appendLine()
            append("Total: ${formatPrice(receipt.totalPrice)} won")
        }
    }

    private fun navigateHome() {
        val popped = findNavController().popBackStack(R.id.fragment_home, false)
        if (!popped) {
            findNavController().navigate(R.id.fragment_home)
        }
    }

    private fun navigatePaymentWaiting() {
        findNavController().navigate(
            R.id.fragment_group_payment_waiting,
            Bundle().apply {
                putString(GroupPaymentWaitingFragment.ARG_GROUP_ID, groupId)
                putLong(GroupPaymentWaitingFragment.ARG_USER_ID, currentUserId)
            }
        )
        isPaymentCompleting = false
    }

    private fun navigatePaymentFailure(reason: String) {
        findNavController().navigate(
            R.id.fragment_payment_failure,
            Bundle().apply {
                putString(PaymentFailureFragment.ARG_TITLE, "결제 실패")
                putString(
                    PaymentFailureFragment.ARG_MESSAGE,
                    reason.ifBlank { "주문 결제가 완료되지 않았어요. 잠시 후 홈으로 이동합니다." }
                )
            }
        )
        isPaymentCompleting = false
    }

    private fun observePaymentDeepLink() {
        val savedStateHandle = findNavController().currentBackStackEntry?.savedStateHandle ?: return
        savedStateHandle.getLiveData<String>(MainActivity.PAYMENT_DEEP_LINK_URI)
            .observe(viewLifecycleOwner) { uriString ->
                savedStateHandle.remove<String>(MainActivity.PAYMENT_DEEP_LINK_URI)
                handlePaymentDeepLink(Uri.parse(uriString))
            }
    }

    private fun handlePaymentDeepLink(uri: Uri) {
        when (uri.path) {
            "/success" -> {
                val paymentKey = uri.getQueryParameter("paymentKey").orEmpty()
                val approvedOrderId = uri.getQueryParameter("orderId").orEmpty()
                val amount = uri.getQueryParameter("amount")?.toLongOrNull() ?: totalPrice

                if (paymentKey.isBlank() || approvedOrderId.isBlank()) {
                    Toast.makeText(requireContext(), "Payment confirmation data is missing.", Toast.LENGTH_SHORT).show()
                    _binding?.btnPayment?.isEnabled = true
                    return
                }

                completePayment(paymentKey, approvedOrderId, amount)
            }

            "/fail" -> {
                val message = uri.getQueryParameter("message") ?: "Payment failed."
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                handlePaymentFailure(message)
            }
        }
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
        binding.paymentPulseRingBack.clearAnimation()
        binding.paymentPulseRingFront.clearAnimation()
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_ORDER_ID = "ORDER_ID"
        const val ARG_TOTAL_PRICE = "TOTAL_PRICE"
        const val ARG_ORDER_NAME = "ORDER_NAME"
        const val ARG_GROUP_ID = "GROUP_ID"
        const val ARG_USER_ID = "USER_ID"
        private const val TAG = "PaymentFragment"
    }
}

private fun formatPrice(value: Long): String {
    return NumberFormat.getNumberInstance(Locale.KOREA).format(value)
}

private val Int.dp: Int
    get() = (this * android.content.res.Resources.getSystem().displayMetrics.density).toInt()

private fun String.toReceiptDateTime(): String {
    return take(16)
        .replace("T", " ")
        .replace("-", ".")
        .replace(" ", " ")
}

private fun String.receiptIconRes(): Int {
    val name = lowercase()
    return when {
        "라떼" in name || "latte" in name || "밀크" in name -> R.drawable.coffee_24px
        "빵" in name || "bread" in name || "베이글" in name -> R.drawable.ic_bread
        "말차" in name || "matcha" in name || "파르페" in name -> R.drawable.ic_matcha_parfait
        else -> R.drawable.ic_cup_line
    }
}

private fun String.maskPaymentKey(): String {
    if (length <= 12) return "***"
    return "${take(6)}...${takeLast(4)}"
}
