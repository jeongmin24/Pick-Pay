package com.ssafy.payclient.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
    private var isPaymentCompleting = false

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
        setupPaymentInfo()
        setupClickListeners()
        observePaymentDeepLink()
    }

    private fun setupToolbar() {
        binding.toolbarPayment.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupPaymentInfo() {
        binding.tvPaymentAmount.text = "$totalPrice 원"
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

    // 결제창 호출 ( Toss Payments SDK )
    private fun requestPayment() {
        val paymentInfo = TossPaymentInfo(
            orderId = orderId,
            orderName = orderName.ifBlank { "Individual order" },
            amount = totalPrice
        )
        val paymentMethod = TossPaymentMethod.Card

        // 실제 결제 팝업 띄우기
        tossPayments.requestPayment(
            requireActivity(),
            paymentMethod,
            paymentInfo,
            paymentResultLauncher // 결제 결과 콜백 함수
        )
    }

    // 결제를 마치거나 취소해서 팝업이 닫힐때 콜백
    private fun handlePaymentResult(resultCode: Int, data: Intent?) {
        when (resultCode) {
            TossPayments.RESULT_PAYMENT_SUCCESS -> {
                val success = data?.getParcelableExtra(TossPayments.EXTRA_PAYMENT_RESULT_SUCCESS)
                    as? TossPaymentResult.Success

                if (success == null) {
                    Toast.makeText(requireContext(), "결제 결과를 확인할 수 없습니다.", Toast.LENGTH_SHORT).show()
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
            }
            else -> Toast.makeText(
                requireContext(),
                "Payment was canceled.",
                Toast.LENGTH_SHORT
            ).show()
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
                Log.d(
                    TAG,
                    "Payment complete request paymentKey=${paymentKey.maskPaymentKey()}, " +
                        "orderId=$approvedOrderId, amount=$amount"
                )
                val response = apiService.completePayment(request)

                if (response.isSuccessful) {
                    val body = response.body()
                    Log.d(
                        TAG,
                        "Payment complete success orderId=${body?.orderId}, " +
                            "amount=${body?.amount}, status=${body?.orderStatus}"
                    )
                    PersonalCartStore.clear()
                    Toast.makeText(
                        requireContext(),
                        body?.message ?: "결제가 완료되었습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                    fetchReceipt(approvedOrderId)
                } else {
                    Log.e(
                        TAG,
                        "Payment complete failed status=${response.code()}, " +
                            "body=${response.errorBody()?.string()}"
                    )
                    Toast.makeText(
                        requireContext(),
                        "결제 승인에 실패했습니다. (${response.code()})",
                        Toast.LENGTH_SHORT
                    ).show()
                    isPaymentCompleting = false
                    _binding?.btnPayment?.isEnabled = true
                }
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "결제 승인 중 오류가 발생했습니다: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                isPaymentCompleting = false
                _binding?.btnPayment?.isEnabled = true
            }
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
                    showReceiptLoadFailure("영수증 응답이 비어 있습니다.")
                    return
                }
                renderReceipt(receipt, orderNo)
            } else {
                Log.e(
                    TAG,
                    "Receipt fetch failed orderNo=$orderNo, status=${response.code()}, " +
                        "body=${response.errorBody()?.string()}"
                )
                showReceiptLoadFailure("영수증 조회에 실패했습니다. (${response.code()})")
            }
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Receipt fetch error orderNo=$orderNo",
                e
            )
            showReceiptLoadFailure("영수증 조회 중 오류가 발생했습니다: ${e.message}")
        }
    }

    private fun renderReceipt(receipt: IndividualReceiptResponseDTO, orderNo: String) {
        binding.toolbarPayment.title = "Receipt"
        binding.tvPaymentAmount.text = "${formatPrice(receipt.totalPrice)} 원"
        binding.tvPaymentDescription.text = "Payment completed"
        binding.tvReceiptDetails.text = buildReceiptText(receipt, orderNo)
        binding.tvReceiptDetails.visibility = View.VISIBLE
        binding.btnPayment.visibility = View.GONE
        binding.btnHome.visibility = View.VISIBLE
        binding.toolbarPayment.setNavigationOnClickListener {
            navigateHome()
        }
        isPaymentCompleting = false
    }

    private fun showReceiptLoadFailure(message: String) {
        binding.tvPaymentDescription.text = message
        binding.btnPayment.visibility = View.GONE
        binding.btnHome.visibility = View.VISIBLE
        binding.toolbarPayment.setNavigationOnClickListener {
            navigateHome()
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        isPaymentCompleting = false
    }

    private fun buildReceiptText(receipt: IndividualReceiptResponseDTO, orderNo: String): String {
        val itemLines = receipt.items.joinToString(separator = "\n") { item ->
            val subtotal = item.price * item.quantity
            "${item.menuName.orEmpty()} x ${item.quantity}  ${formatPrice(subtotal)} 원"
        }

        return buildString {
            appendLine("Order No. $orderNo")
            appendLine("Status: ${receipt.status}")
            appendLine("Ordered at: ${receipt.createdAt}")
            appendLine()
            appendLine("Items")
            appendLine(if (itemLines.isBlank()) "No items" else itemLines)
            appendLine()
            append("Total: ${formatPrice(receipt.totalPrice)} 원")
        }
    }

    private fun navigateHome() {
        val popped = findNavController().popBackStack(R.id.fragment_home, false)
        if (!popped) {
            findNavController().navigate(R.id.fragment_home)
        }
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
                    Toast.makeText(requireContext(), "결제 승인 정보를 확인할 수 없습니다.", Toast.LENGTH_SHORT).show()
                    _binding?.btnPayment?.isEnabled = true
                    return
                }

                completePayment(paymentKey, approvedOrderId, amount)
            }
            "/fail" -> {
                val message = uri.getQueryParameter("message") ?: "결제에 실패했습니다."
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                _binding?.btnPayment?.isEnabled = true
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
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_ORDER_ID = "ORDER_ID"
        const val ARG_TOTAL_PRICE = "TOTAL_PRICE"
        const val ARG_ORDER_NAME = "ORDER_NAME"
        private const val TAG = "PaymentFragment"
    }
}

private fun formatPrice(value: Long): String {
    return NumberFormat.getNumberInstance(Locale.KOREA).format(value)
}

private fun String.maskPaymentKey(): String {
    if (length <= 12) return "***"
    return "${take(6)}...${takeLast(4)}"
}
