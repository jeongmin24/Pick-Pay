package com.ssafy.payclient.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ssafy.payclient.R
import com.ssafy.payclient.databinding.FragmentPaymentFailureBinding

class PaymentFailureFragment : Fragment() {

    private var _binding: FragmentPaymentFailureBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())
    private val navigateHomeRunnable = Runnable { navigateHome() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaymentFailureBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvPaymentFailureTitle.text =
            arguments?.getString(ARG_TITLE).orEmpty().ifBlank { "결제 실패" }
        binding.tvPaymentFailureMessage.text =
            arguments?.getString(ARG_MESSAGE).orEmpty().ifBlank {
                "주문 결제가 완료되지 않았어요. 잠시 후 홈으로 이동합니다."
            }
        binding.btnPaymentFailureHome.setOnClickListener {
            navigateHome()
        }

        handler.postDelayed(navigateHomeRunnable, AUTO_HOME_DELAY_MILLIS)
    }

    private fun navigateHome() {
        if (_binding == null) return
        handler.removeCallbacks(navigateHomeRunnable)
        val popped = findNavController().popBackStack(R.id.fragment_home, false)
        if (!popped) {
            findNavController().navigate(R.id.fragment_home)
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
        handler.removeCallbacks(navigateHomeRunnable)
        _binding = null
    }

    companion object {
        const val ARG_TITLE = "TITLE"
        const val ARG_MESSAGE = "MESSAGE"
        private const val AUTO_HOME_DELAY_MILLIS = 3_000L
    }
}
