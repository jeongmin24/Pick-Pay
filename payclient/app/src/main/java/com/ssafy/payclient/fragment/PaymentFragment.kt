package com.ssafy.payclient.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ssafy.payclient.BuildConfig
import com.ssafy.payclient.R
import com.ssafy.payclient.data.local.PersonalCartStore
import com.ssafy.payclient.databinding.FragmentPaymentBinding
import com.tosspayments.paymentsdk.TossPayments
import com.tosspayments.paymentsdk.model.TossPaymentResult
import com.tosspayments.paymentsdk.model.paymentinfo.TossPaymentInfo
import com.tosspayments.paymentsdk.model.paymentinfo.TossPaymentMethod

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
    }

    private fun setupToolbar() {
        binding.toolbarPayment.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupPaymentInfo() {
        binding.tvPaymentAmount.text = "$totalPrice 원"
        binding.btnPayment.isEnabled = true
    }

    private fun setupClickListeners() {
        binding.btnPayment.setOnClickListener {
            requestPayment()
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
            // success -> paymentKey, orderId, amount를 서버로 보내기 ( 결제 승인 요청 )
            TossPayments.RESULT_PAYMENT_SUCCESS -> {
                val success = data?.getParcelableExtra(TossPayments.EXTRA_PAYMENT_RESULT_SUCCESS)
                    as? TossPaymentResult.Success

                // success 가 null 이 아니면
                // 1. 로딩 인디케이터 표시 및 버튼 비활성화
                // 2. 서버로 최종 결제승인 요청 /api/payments
                // 2-1. 서버 승인까지 완벽히 성공했을때 장바구니 빙귀
                // 2-2. 서버에서 승인 거절 된 경우 or 네트워크 오류 등 예외시 btnPayment.isEnable = true
                PersonalCartStore.clear()
                Toast.makeText(
                    requireContext(),
                    "Payment completed. ${success?.orderId.orEmpty()}",
                    Toast.LENGTH_SHORT
                ).show()
                findNavController().popBackStack(R.id.fragment_order, false)
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
    }
}
